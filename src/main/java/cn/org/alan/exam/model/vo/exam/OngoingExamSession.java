package cn.org.alan.exam.model.vo.exam;

import java.io.Serializable;

import lombok.Data;

/**
 * 进行中考试会话（用于自动交卷 Redis 缓存）
 */
@Data
public class OngoingExamSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /** t_user_exams_score.id */
    private Integer id;
    private Integer userId;
    private Integer examId;
    /** 截止交卷时间（epoch milli） */
    private Long deadlineEpochMilli;
}
