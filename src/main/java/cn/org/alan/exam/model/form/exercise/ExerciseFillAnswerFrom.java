package cn.org.alan.exam.model.form.exercise;


import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * @Author WeiJin
 * @Version 1.0
 * @Date 2024/5/6 9:52
 */
@Data
public class ExerciseFillAnswerFrom {
    // 题库ID
    @NotNull(message = "题库Id不能为空")
    private Integer repoId;
    // 试题ID
    @NotNull(message = "试题Id不能为空")
    private Integer quId;
    // 作答内容（允许空提交，便于直接查看正确答案与解析）
    private String answer;
    // 试题类型
    @NotNull(message = "试题类型不能为空")
    @Min(value = 1, message = "试题类型最小值应为1")
    @Max(value = 5, message = "试题类型最大值应为5")
    private Integer quType;
}
