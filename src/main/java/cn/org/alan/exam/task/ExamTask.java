package cn.org.alan.exam.task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import cn.org.alan.exam.common.cache.OngoingExamCacheService;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.CertificateUserMapper;
import cn.org.alan.exam.mapper.ExamMapper;
import cn.org.alan.exam.mapper.ExamQuAnswerMapper;
import cn.org.alan.exam.mapper.ExamQuestionMapper;
import cn.org.alan.exam.mapper.UserBookMapper;
import cn.org.alan.exam.mapper.UserExamsScoreMapper;
import cn.org.alan.exam.model.entity.CertificateUser;
import cn.org.alan.exam.model.entity.Exam;
import cn.org.alan.exam.model.entity.ExamQuAnswer;
import cn.org.alan.exam.model.entity.ExamQuestion;
import cn.org.alan.exam.model.entity.UserBook;
import cn.org.alan.exam.model.entity.UserExamsScore;
import cn.org.alan.exam.model.enums.ExamState;
import cn.org.alan.exam.model.vo.exam.ExamQuDetailVO;
import cn.org.alan.exam.model.vo.exam.OngoingExamSession;
import cn.org.alan.exam.service.IAutoScoringService;
import cn.org.alan.exam.utils.ClassTokenGenerator;
import lombok.extern.slf4j.Slf4j;

/**
 * 自动交卷定时任务：5 秒轮询 Redis 到期场次；约每分钟与数据库对账。
 */
@Component
@Slf4j
public class ExamTask {
    @Resource
    private ExamQuAnswerMapper examQuAnswerMapper;
    @Resource
    private UserExamsScoreMapper userExamsScoreMapper;
    @Resource
    private UserBookMapper userBookMapper;
    @Resource
    private ExamQuestionMapper examQuestionMapper;
    @Resource
    private CertificateUserMapper certificateUserMapper;
    @Resource
    private ExamMapper examMapper;
    @Resource
    private IAutoScoringService autoScoringService;
    @Resource
    private OngoingExamCacheService ongoingExamCacheService;

    /**
     * 每 5 秒检查是否有超时未交卷的进行中考试，自动交卷。
     */
    @Scheduled(initialDelay = 1000, fixedDelay = 5 * 1000)
    public void autoHandOverdueExams() {
        LocalDateTime now = LocalDateTime.now();
        List<OngoingExamSession> dueList = ongoingExamCacheService.listDue(now);
        if (dueList == null || dueList.isEmpty()) {
            return;
        }
        for (OngoingExamSession session : dueList) {
            try {
                UserExamsScore record = resolveOngoingRecord(session);
                if (record == null) {
                    ongoingExamCacheService.untrack(session.getUserId(), session.getExamId());
                    continue;
                }
                handExam(record);
                ongoingExamCacheService.untrack(record.getUserId(), record.getExamId());
                log.info("自动交卷成功，用户ID: {}, 考试ID: {}", record.getUserId(), record.getExamId());
            } catch (Exception e) {
                log.error("自动交卷处理异常，userId={}, examId={}", session.getUserId(), session.getExamId(), e);
            }
        }
    }

    private UserExamsScore resolveOngoingRecord(OngoingExamSession session) {
        if (session.getId() != null) {
            UserExamsScore byId = userExamsScoreMapper.selectById(session.getId());
            if (byId != null && byId.getState() != null
                    && byId.getState() == ExamState.ONGOING.getCode()) {
                return byId;
            }
            if (byId != null) {
                return null;
            }
        }
        LambdaQueryWrapper<UserExamsScore> query = new LambdaQueryWrapper<>();
        query.eq(UserExamsScore::getUserId, session.getUserId())
                .eq(UserExamsScore::getExamId, session.getExamId())
                .eq(UserExamsScore::getState, ExamState.ONGOING.getCode())
                .last("limit 1");
        return userExamsScoreMapper.selectOne(query);
    }

    /**
     * 交卷操作
     */
    @Transactional
    public Result<ExamQuDetailVO> handExam(UserExamsScore ues) {
        LocalDateTime nowTime = LocalDateTime.now();
        Exam examOne = examMapper.selectById(ues.getExamId());
        if (examOne == null) {
            ongoingExamCacheService.untrack(ues.getUserId(), ues.getExamId());
            return Result.failed("考试不存在");
        }

        // 幂等：仅处理进行中记录
        UserExamsScore latest = userExamsScoreMapper.selectById(ues.getId());
        if (latest == null || latest.getState() == null
                || latest.getState() != ExamState.ONGOING.getCode()) {
            ongoingExamCacheService.untrack(ues.getUserId(), ues.getExamId());
            return Result.success("已交卷");
        }

        UserExamsScore userExamsScore = new UserExamsScore();
        userExamsScore.setUserScore(0);
        userExamsScore.setState(1);

        LambdaQueryWrapper<ExamQuAnswer> examQuAnswerLambdaQuery = new LambdaQueryWrapper<>();
        examQuAnswerLambdaQuery.eq(ExamQuAnswer::getUserId, ues.getUserId())
                .eq(ExamQuAnswer::getExamId, ues.getExamId());
        List<ExamQuAnswer> examQuAnswer = examQuAnswerMapper.selectList(examQuAnswerLambdaQuery);
        LambdaQueryWrapper<ExamQuestion> eqWrapper = new LambdaQueryWrapper<>();
        eqWrapper.eq(ExamQuestion::getExamId, ues.getExamId());
        Map<Integer, Integer> quScoreMap = examQuestionMapper.selectList(eqWrapper).stream()
                .collect(Collectors.toMap(ExamQuestion::getQuestionId, ExamQuestion::getScore, (a, b) -> a));

        List<UserBook> userBookArrayList = new ArrayList<>();
        for (ExamQuAnswer temp : examQuAnswer) {
            if (temp.getIsRight() != null && temp.getIsRight() == 1) {
                Integer quScore = quScoreMap.get(temp.getQuestionId());
                if (quScore != null) {
                    userExamsScore.setUserScore(userExamsScore.getUserScore() + quScore);
                } else if (temp.getQuestionType() != null && temp.getQuestionType() == 1) {
                    userExamsScore.setUserScore(userExamsScore.getUserScore() + examOne.getRadioScore());
                } else if (temp.getQuestionType() != null && temp.getQuestionType() == 2) {
                    userExamsScore.setUserScore(userExamsScore.getUserScore() + examOne.getMultiScore());
                } else if (temp.getQuestionType() != null && temp.getQuestionType() == 3) {
                    userExamsScore.setUserScore(userExamsScore.getUserScore() + examOne.getJudgeScore());
                }
            } else if (temp.getIsRight() != null && temp.getIsRight() == 0) {
                UserBook userBook = new UserBook();
                userBook.setExamId(ues.getExamId());
                userBook.setUserId(ues.getUserId());
                userBook.setQuId(temp.getQuestionId());
                userBook.setCreateTime(nowTime);
                userBookArrayList.add(userBook);
            }
        }
        if (!userBookArrayList.isEmpty()) {
            userBookMapper.addUserBookList(userBookArrayList);
        }

        userExamsScore.setLimitTime(nowTime);
        LocalDateTime createTime = latest.getCreateTime();
        if (createTime == null) {
            createTime = nowTime;
        }
        long secondsDifference = Duration.between(createTime, nowTime).getSeconds();
        userExamsScore.setUserTime((int) secondsDifference);

        LambdaUpdateWrapper<UserExamsScore> userExamsScoreLambdaUpdate = new LambdaUpdateWrapper<>();
        userExamsScoreLambdaUpdate.eq(UserExamsScore::getId, latest.getId())
                .eq(UserExamsScore::getState, ExamState.ONGOING.getCode());
        int updated = userExamsScoreMapper.update(userExamsScore, userExamsScoreLambdaUpdate);
        if (updated < 1) {
            ongoingExamCacheService.untrack(ues.getUserId(), ues.getExamId());
            return Result.success("已交卷");
        }

        if (examOne.getSaqCount() != null && examOne.getSaqCount() != 0) {
            LambdaUpdateWrapper<UserExamsScore> markWrapper = new LambdaUpdateWrapper<>();
            markWrapper.set(UserExamsScore::getWhetherMark, 0)
                    .eq(UserExamsScore::getId, latest.getId());
            userExamsScoreMapper.update(null, markWrapper);
            autoScoringService.autoScoringExam(ues.getExamId(), ues.getUserId());
            ongoingExamCacheService.untrack(ues.getUserId(), ues.getExamId());
            return Result.success("提交成功，待老师阅卷");
        }
        if (userExamsScore.getUserScore() >= examOne.getPassedScore()) {
            CertificateUser certificateUser = new CertificateUser();
            certificateUser.setCertificateId(examOne.getCertificateId());
            certificateUser.setUserId(ues.getUserId());
            certificateUser.setExamId(ues.getExamId());
            certificateUser.setCode(ClassTokenGenerator.generateClassToken(18));
            certificateUserMapper.insert(certificateUser);
        }

        if (examOne.getSaqCount() != null && examOne.getSaqCount() > 0) {
            LambdaQueryWrapper<ExamQuAnswer> saqAnswerQuery = new LambdaQueryWrapper<>();
            saqAnswerQuery.eq(ExamQuAnswer::getUserId, ues.getUserId())
                    .eq(ExamQuAnswer::getExamId, ues.getExamId())
                    .eq(ExamQuAnswer::getQuestionType, 4);
            List<ExamQuAnswer> examQuAnswers = examQuAnswerMapper.selectList(saqAnswerQuery);
            if (examQuAnswers.isEmpty()) {
                LambdaQueryWrapper<ExamQuestion> examQuestionQuery = new LambdaQueryWrapper<>();
                examQuestionQuery.eq(ExamQuestion::getExamId, ues.getExamId())
                        .eq(ExamQuestion::getType, 4);
                List<ExamQuestion> examQuestions = examQuestionMapper.selectList(examQuestionQuery);
                examQuestions.forEach(temp -> {
                    ExamQuAnswer examQuAnswer1 = new ExamQuAnswer();
                    examQuAnswer1.setExamId(ues.getExamId());
                    examQuAnswer1.setUserId(ues.getUserId());
                    examQuAnswer1.setQuestionId(temp.getQuestionId());
                    examQuAnswer1.setQuestionType(temp.getType());
                    examQuAnswer1.setIsRight(-1);
                    examQuAnswerMapper.insert(examQuAnswer1);
                });
            }
        }

        LambdaUpdateWrapper<UserExamsScore> doneMark = new LambdaUpdateWrapper<>();
        doneMark.set(UserExamsScore::getWhetherMark, -1)
                .eq(UserExamsScore::getId, latest.getId());
        userExamsScoreMapper.update(null, doneMark);
        ongoingExamCacheService.untrack(ues.getUserId(), ues.getExamId());
        return Result.success("交卷成功");
    }
}
