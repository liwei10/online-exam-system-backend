package cn.org.alan.exam.common.cache;

import cn.org.alan.exam.mapper.QuestionMapper;
import cn.org.alan.exam.model.entity.Option;
import cn.org.alan.exam.model.vo.exam.OptionVO;
import cn.org.alan.exam.model.vo.question.QuContentShell;
import cn.org.alan.exam.model.vo.question.QuestionVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 题目内容壳缓存：考试/刷题共用，不含正确答案与用户态
 */
@Service
public class QuContentCacheService {

    private static final long TTL_HOURS = 6;

    @Resource
    private CacheService cacheService;
    @Resource
    private QuestionMapper questionMapper;

    /**
     * 获取题目内容壳（优先 Redis）
     */
    public QuContentShell getShell(Integer quId) {
        if (quId == null) {
            return null;
        }
        String key = CacheKeys.quContent(quId);
        QuContentShell cached = cacheService.get(key);
        if (cached != null) {
            return cached;
        }
        QuContentShell shell = loadFromDb(quId);
        if (shell != null) {
            cacheService.set(key, shell, TTL_HOURS, TimeUnit.HOURS);
        }
        return shell;
    }

    /**
     * 转为刷题用 QuestionVO（选项无 isRight）
     */
    public QuestionVO toQuestionVO(QuContentShell shell) {
        if (shell == null) {
            return null;
        }
        QuestionVO vo = new QuestionVO();
        vo.setId(shell.getId());
        vo.setContent(shell.getContent());
        vo.setImage(shell.getImage());
        vo.setAudio(shell.getAudio());
        vo.setQuType(shell.getQuType());
        vo.setRepoId(shell.getRepoId());
        vo.setRepoTitle(shell.getRepoTitle());
        if (shell.getOptions() == null) {
            vo.setOptions(Collections.emptyList());
        } else {
            List<Option> options = shell.getOptions().stream().map(o -> {
                Option opt = new Option();
                opt.setId(o.getId());
                opt.setQuId(o.getQuId());
                opt.setImage(o.getImage());
                opt.setContent(o.getContent());
                opt.setSort(o.getSort());
                return opt;
            }).collect(Collectors.toList());
            vo.setOptions(options);
        }
        return vo;
    }

    /**
     * 转为考试选项 VO（无 checkout）
     */
    public List<OptionVO> toOptionVOList(QuContentShell shell) {
        if (shell == null || shell.getOptions() == null) {
            return new ArrayList<>();
        }
        return shell.getOptions().stream().map(o -> {
            OptionVO vo = new OptionVO();
            vo.setId(o.getId());
            vo.setQuId(o.getQuId());
            vo.setImage(o.getImage());
            vo.setContent(o.getContent());
            vo.setSort(o.getSort());
            vo.setCheckout(false);
            return vo;
        }).collect(Collectors.toList());
    }

    public void evict(Integer quId) {
        if (quId != null) {
            cacheService.delete(CacheKeys.quContent(quId));
        }
    }

    public void evictBatch(List<Integer> quIds) {
        if (quIds == null) {
            return;
        }
        for (Integer quId : quIds) {
            evict(quId);
        }
    }

    private QuContentShell loadFromDb(Integer quId) {
        // selectDetail 选项已不含 is_right
        QuestionVO detail = questionMapper.selectDetail(quId);
        if (detail == null) {
            return null;
        }
        QuContentShell shell = new QuContentShell();
        shell.setId(detail.getId());
        shell.setContent(detail.getContent());
        shell.setImage(detail.getImage());
        shell.setAudio(detail.getAudio());
        shell.setQuType(detail.getQuType());
        shell.setRepoId(detail.getRepoId());
        shell.setRepoTitle(detail.getRepoTitle());

        List<Option> options = detail.getOptions();
        if (options == null) {
            shell.setOptions(Collections.emptyList());
        } else {
            List<QuContentShell.OptionShell> optionShells = options.stream().map(o -> {
                QuContentShell.OptionShell os = new QuContentShell.OptionShell();
                os.setId(o.getId());
                os.setQuId(o.getQuId());
                os.setImage(o.getImage());
                os.setContent(o.getContent());
                os.setSort(o.getSort());
                return os;
            }).collect(Collectors.toList());
            shell.setOptions(optionShells);
        }
        return shell;
    }
}
