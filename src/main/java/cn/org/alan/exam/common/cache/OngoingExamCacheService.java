package cn.org.alan.exam.common.cache;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import cn.org.alan.exam.mapper.ExamMapper;
import cn.org.alan.exam.mapper.UserExamsScoreMapper;
import cn.org.alan.exam.model.entity.Exam;
import cn.org.alan.exam.model.entity.UserExamsScore;
import cn.org.alan.exam.model.enums.ExamState;
import cn.org.alan.exam.model.vo.exam.OngoingExamSession;
import lombok.extern.slf4j.Slf4j;

/**
 * 进行中考试 Redis 缓存：定时任务优先读 Redis 判超时，约每分钟与数据库对账一次。
 */
@Service
@Slf4j
public class OngoingExamCacheService {

    private static final long RECONCILE_INTERVAL_MS = 60_000L;

    @Resource
    private RedisTemplate redisTemplateInit;
    @Resource
    private UserExamsScoreMapper userExamsScoreMapper;
    @Resource
    private ExamMapper examMapper;

    private final AtomicLong lastReconcileAt = new AtomicLong(0);

    private static String memberKey(Integer userId, Integer examId) {
        return userId + ":" + examId;
    }

    /**
     * 开考时写入；交卷时删除。
     */
    public void track(Integer recordId, Integer userId, Integer examId,
                      LocalDateTime startTime, Integer durationMinutes) {
        if (recordId == null || userId == null || examId == null
                || startTime == null || durationMinutes == null || durationMinutes < 0) {
            return;
        }
        long deadlineMs = startTime.plusMinutes(durationMinutes)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        OngoingExamSession session = new OngoingExamSession();
        session.setId(recordId);
        session.setUserId(userId);
        session.setExamId(examId);
        session.setDeadlineEpochMilli(deadlineMs);
        String member = memberKey(userId, examId);
        try {
            redisTemplateInit.opsForHash().put(CacheKeys.EXAM_ONGOING_HASH, member, session);
            redisTemplateInit.opsForZSet().add(CacheKeys.EXAM_ONGOING_ZSET, member, deadlineMs);
        } catch (Exception e) {
            log.warn("写入进行中考试缓存失败 userId={} examId={}", userId, examId, e);
        }
    }

    public void track(UserExamsScore record, Exam exam) {
        if (record == null || exam == null) {
            return;
        }
        Integer duration = record.getTotalTime() != null ? record.getTotalTime() : exam.getExamDuration();
        track(record.getId(), record.getUserId(), record.getExamId(), record.getCreateTime(), duration);
    }

    public void untrack(Integer userId, Integer examId) {
        if (userId == null || examId == null) {
            return;
        }
        String member = memberKey(userId, examId);
        try {
            redisTemplateInit.opsForZSet().remove(CacheKeys.EXAM_ONGOING_ZSET, member);
            redisTemplateInit.opsForHash().delete(CacheKeys.EXAM_ONGOING_HASH, member);
        } catch (Exception e) {
            log.warn("删除进行中考试缓存失败 userId={} examId={}", userId, examId, e);
        }
    }

    /**
     * 取出已到截止时间的进行中场次；必要时先与数据库对账。
     */
    @SuppressWarnings("unchecked")
    public List<OngoingExamSession> listDue(LocalDateTime now) {
        reconcileIfNeeded();
        long nowMs = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Set<Object> members;
        try {
            members = redisTemplateInit.opsForZSet().rangeByScore(CacheKeys.EXAM_ONGOING_ZSET, 0, nowMs);
        } catch (Exception e) {
            log.error("读取到期考试缓存失败，回退数据库", e);
            return loadDueFromDb(now);
        }
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }
        List<OngoingExamSession> result = new ArrayList<>();
        for (Object memberObj : members) {
            String member = String.valueOf(memberObj);
            Object raw = redisTemplateInit.opsForHash().get(CacheKeys.EXAM_ONGOING_HASH, member);
            OngoingExamSession session = toSession(raw);
            if (session != null) {
                result.add(session);
            } else {
                // hash 丢失时用 member 解析后仍交卷（再查库）
                String[] parts = member.split(":");
                if (parts.length == 2) {
                    OngoingExamSession fallback = new OngoingExamSession();
                    fallback.setUserId(Integer.valueOf(parts[0]));
                    fallback.setExamId(Integer.valueOf(parts[1]));
                    fallback.setDeadlineEpochMilli(nowMs);
                    result.add(fallback);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private OngoingExamSession toSession(Object raw) {
        if (raw instanceof OngoingExamSession) {
            return (OngoingExamSession) raw;
        }
        if (raw instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) raw;
            OngoingExamSession session = new OngoingExamSession();
            Object id = map.get("id");
            Object userId = map.get("userId");
            Object examId = map.get("examId");
            Object deadline = map.get("deadlineEpochMilli");
            if (id != null) {
                session.setId(Integer.valueOf(String.valueOf(id)));
            }
            if (userId != null) {
                session.setUserId(Integer.valueOf(String.valueOf(userId)));
            }
            if (examId != null) {
                session.setExamId(Integer.valueOf(String.valueOf(examId)));
            }
            if (deadline != null) {
                session.setDeadlineEpochMilli(Long.valueOf(String.valueOf(deadline)));
            }
            return session.getUserId() != null && session.getExamId() != null ? session : null;
        }
        return null;
    }

    /**
     * 约每分钟全量对账；首次启动立即对账。
     */
    public void reconcileIfNeeded() {
        long now = System.currentTimeMillis();
        long last = lastReconcileAt.get();
        if (last > 0 && now - last < RECONCILE_INTERVAL_MS) {
            return;
        }
        if (!lastReconcileAt.compareAndSet(last, now) && last > 0) {
            return;
        }
        try {
            rebuildFromDb();
            lastReconcileAt.set(System.currentTimeMillis());
        } catch (Exception e) {
            lastReconcileAt.set(last);
            log.error("进行中考试缓存对账失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void rebuildFromDb() {
        LambdaQueryWrapper<UserExamsScore> query = new LambdaQueryWrapper<>();
        query.eq(UserExamsScore::getState, ExamState.ONGOING.getCode());
        List<UserExamsScore> ongoing = userExamsScoreMapper.selectList(query);
        redisTemplateInit.delete(CacheKeys.EXAM_ONGOING_ZSET);
        redisTemplateInit.delete(CacheKeys.EXAM_ONGOING_HASH);
        if (ongoing == null || ongoing.isEmpty()) {
            return;
        }
        Set<Integer> examIds = ongoing.stream().map(UserExamsScore::getExamId).collect(Collectors.toSet());
        Map<Integer, Exam> examMap = examIds.isEmpty()
                ? Collections.emptyMap()
                : examMapper.selectBatchIds(examIds).stream()
                .collect(Collectors.toMap(Exam::getId, e -> e, (a, b) -> a));
        for (UserExamsScore record : ongoing) {
            Exam exam = examMap.get(record.getExamId());
            if (exam == null) {
                continue;
            }
            Integer duration = record.getTotalTime() != null ? record.getTotalTime() : exam.getExamDuration();
            if (record.getCreateTime() == null || duration == null) {
                continue;
            }
            track(record.getId(), record.getUserId(), record.getExamId(), record.getCreateTime(), duration);
        }
        log.debug("进行中考试缓存对账完成，共 {} 条", ongoing.size());
    }

    private List<OngoingExamSession> loadDueFromDb(LocalDateTime now) {
        LambdaQueryWrapper<UserExamsScore> query = new LambdaQueryWrapper<>();
        query.eq(UserExamsScore::getState, ExamState.ONGOING.getCode());
        List<UserExamsScore> ongoing = userExamsScoreMapper.selectList(query);
        if (ongoing == null || ongoing.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Integer> examIds = ongoing.stream().map(UserExamsScore::getExamId).collect(Collectors.toSet());
        Map<Integer, Exam> examMap = examMapper.selectBatchIds(examIds).stream()
                .collect(Collectors.toMap(Exam::getId, e -> e, (a, b) -> a));
        List<OngoingExamSession> due = new ArrayList<>();
        for (UserExamsScore record : ongoing) {
            Exam exam = examMap.get(record.getExamId());
            if (exam == null || record.getCreateTime() == null) {
                continue;
            }
            int duration = record.getTotalTime() != null ? record.getTotalTime() : exam.getExamDuration();
            LocalDateTime deadline = record.getCreateTime().plusMinutes(duration);
            if (now.isAfter(deadline)) {
                OngoingExamSession session = new OngoingExamSession();
                session.setId(record.getId());
                session.setUserId(record.getUserId());
                session.setExamId(record.getExamId());
                session.setDeadlineEpochMilli(deadline.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                due.add(session);
            }
        }
        return due;
    }
}
