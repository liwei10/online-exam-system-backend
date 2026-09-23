package cn.org.alan.exam.model.vo.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 题目内容缓存壳：题干 + 选项展示信息，不含 isRight / 用户作答
 */
@Data
public class QuContentShell implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String content;
    private String image;
    private String audio;
    private Integer quType;
    private Integer repoId;
    private String repoTitle;
    private List<OptionShell> options;

    @Data
    public static class OptionShell implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer id;
        private Integer quId;
        private String image;
        private String content;
        private Integer sort;
    }
}
