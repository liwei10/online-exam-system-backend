package cn.org.alan.exam.model.vo.exercise;

import lombok.Data;

/**
 * 学生刷题可用的题库分类
 */
@Data
public class ExerciseCategoryVO {
    private Integer id;
    private String name;
    private Integer parentId;
    private String parentName;
}
