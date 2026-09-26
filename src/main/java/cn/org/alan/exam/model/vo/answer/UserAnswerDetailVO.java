package cn.org.alan.exam.model.vo.answer;

import lombok.Data;

/**
 * 用户作答信息
 *
 * @Author WeiJin
 * @Version 1.0
 * @Date 2024/4/29 10:29
 */
@Data
public class UserAnswerDetailVO {
    // 试题ID
    private Integer quId;
    // 用户ID
    private Integer userId;
    // 试卷ID
    private Integer examId;
    // 试题标题
    private String quTitle;
    // 试题图片
    private String quImg;
    private String answer;
    private String refAnswer;
    /** 当前用于批改的分数（已阅取人工分，否则预填自动分） */
    private Integer correctScore;
    /** AI 评分（简答题） */
    private Integer aiScore;
    /** 填空题按空自动得分 */
    private Integer earnedScore;
    /** AI 扣分/评分说明 */
    private String aiReason;
    /** 人工阅卷得分（已批改时有值） */
    private Integer manualScore;
    /** 试卷是否允许填空二次人工阅卷：1 是 0 否 */
    private Integer fillNeedMark;
    private Integer totalScore;
    /**
     * 试题类型 4简答 5填空
     */
    private Integer quType;

}
