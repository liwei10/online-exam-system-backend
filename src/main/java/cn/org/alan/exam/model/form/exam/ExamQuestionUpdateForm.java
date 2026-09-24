package cn.org.alan.exam.model.form.exam;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * 考试试题更新请求体（替换试卷全部试题并重算总分）
 */
@Data
public class ExamQuestionUpdateForm {

    /**
     * 试题ID，逗号分隔
     */
    @NotBlank(message = "试题不能为空")
    private String quIds;

    @NotNull(message = "单选题分数不能为空")
    @Min(value = 0)
    private Integer radioScore;

    @NotNull(message = "多选题分数不能为空")
    @Min(value = 0)
    private Integer multiScore;

    @NotNull(message = "判断题分数不能为空")
    @Min(value = 0)
    private Integer judgeScore;

    @NotNull(message = "简答题分数不能为空")
    @Min(value = 0)
    private Integer saqScore;

    /**
     * 单题分值，格式：questionId:score,questionId:score
     * 未传则回退到对应题型默认分
     */
    private String quScores;
}
