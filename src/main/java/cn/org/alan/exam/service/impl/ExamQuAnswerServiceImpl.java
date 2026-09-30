package cn.org.alan.exam.service.impl;

import java.text.DecimalFormat;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.ExamQuAnswerMapper;
import cn.org.alan.exam.model.entity.ExamQuAnswer;
import cn.org.alan.exam.model.vo.score.QuestionAnalyseVO;
import cn.org.alan.exam.service.IExamQuAnswerService;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author WeiJin
 * @since 2024-03-21
 */
@Service
public class ExamQuAnswerServiceImpl extends ServiceImpl<ExamQuAnswerMapper, ExamQuAnswer> implements IExamQuAnswerService {

    @Resource
    private ExamQuAnswerMapper examQuAnswerMapper;

    @Override
    public Result<QuestionAnalyseVO> questionAnalyse(Integer examId, Integer questionId) {
        QuestionAnalyseVO questionAnalyseVO = examQuAnswerMapper.questionAnalyse(examId, questionId);
        //正确率保留两位小数
        DecimalFormat format = new DecimalFormat("#.00");
        String strAccuracy = format.format(questionAnalyseVO.getRightCount() / questionAnalyseVO.getTotalCount());
        questionAnalyseVO.setAccuracy(Double.parseDouble(strAccuracy));
        return Result.success(null, questionAnalyseVO);
    }

}
