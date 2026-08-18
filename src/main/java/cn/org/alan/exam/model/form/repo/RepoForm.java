package cn.org.alan.exam.model.form.repo;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import lombok.Data;

/**
 * 题库新增/修改请求体
 */
@Data
public class RepoForm {

    @NotBlank(message = "题库名不能为空")
    private String title;

    /**
     * 是否可以刷题 0否 1是
     */
    private Integer isExercise;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 绑定班级，格式 1,2,3。不传或空表示不对学生开放刷题，仅供组卷
     */
    @Pattern(regexp = "^$|^\\d+(,\\d+)*$", message = "班级参数错误，请将传输格式改为 1,2,3,4...")
    private String gradeIds;
}
