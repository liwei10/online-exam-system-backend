package cn.org.alan.exam.model.vo.grade;

import lombok.Data;

/**
 * 班级关联教师简要信息
 */
@Data
public class GradeTeacherVO {
    private Integer id;
    private String userName;
    private String realName;
}
