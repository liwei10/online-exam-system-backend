package cn.org.alan.exam.model.vo.exam;

import java.util.List;

import lombok.Data;

/**
 * @Author Alan
 * @Version
 * @Date 2024/5/11 9:45 AM
 */
@Data
public class ExamQuestionListVO {
    // 单选题列表
    private List<ExamQuestionVO> radioList;
    // 多选题列表
    private List<ExamQuestionVO> multiList;
    // 判断题列表
    private List<ExamQuestionVO> judgeList;
    // 简答题列表
    private List<ExamQuestionVO> saqList;
    private Integer examDuration;
    public Long leftSeconds;
}
