package cn.org.alan.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.entity.ExamQuAnswer;
import cn.org.alan.exam.model.vo.score.QuestionAnalyseVO;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author WeiJin
 * @since 2024-03-21
 */
public interface IExamQuAnswerService extends IService<ExamQuAnswer> {

    /**
     * 获取某场考试某题作答情况
     * @param examId 考试id
     * @param questionId 试题id
     * @return 结果
     */
    Result<QuestionAnalyseVO> questionAnalyse(Integer examId, Integer questionId);

}
