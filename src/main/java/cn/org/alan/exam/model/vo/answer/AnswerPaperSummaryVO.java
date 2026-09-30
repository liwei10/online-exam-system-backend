package cn.org.alan.exam.model.vo.answer;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 阅卷/查看答卷浮动摘要
 */
@Data
public class AnswerPaperSummaryVO {
    private Integer examId;
    private Integer userId;
    private String examTitle;
    private String userName;
    private String gradeName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime limitTime;
    /** 用时（秒） */
    private Integer userTime;
    private Integer userScore;
    private Integer whetherMark;
}
