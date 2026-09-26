package cn.org.alan.exam.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 填空题占位符与答案比对工具
 * 占位符格式：{{1}}、{{2}}…
 * 学生多空答案分隔：|||
 * 同义答案分隔：|
 */
public final class BlankPlaceholderUtil {

    public static final String ANSWER_DELIMITER = "|||";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\d+)\\}\\}");

    private BlankPlaceholderUtil() {
    }

    /**
     * 按出现顺序解析题干中的占位符序号
     */
    public static List<Integer> parseBlankIndexes(String content) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> indexes = new ArrayList<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        while (matcher.find()) {
            indexes.add(Integer.parseInt(matcher.group(1)));
        }
        return indexes;
    }

    public static int countBlanks(String content) {
        return parseBlankIndexes(content).size();
    }

    /**
     * 校验占位符：数量>0、从1连续编号、与答案条数一致
     */
    public static String validate(String content, int optionCount) {
        List<Integer> indexes = parseBlankIndexes(content);
        if (indexes.isEmpty()) {
            return "填空题题干至少需要一个占位符，格式如 {{1}}";
        }
        if (indexes.size() != optionCount) {
            return "填空题占位符数量(" + indexes.size() + ")与答案条数(" + optionCount + ")不一致";
        }
        for (int i = 0; i < indexes.size(); i++) {
            if (indexes.get(i) != i + 1) {
                return "填空题占位符必须从 {{1}} 起连续编号，当前第" + (i + 1) + "个为 {{" + indexes.get(i) + "}}";
            }
        }
        return null;
    }

    public static List<String> splitAnswers(String answerContent) {
        if (answerContent == null || answerContent.isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = answerContent.split(Pattern.quote(ANSWER_DELIMITER), -1);
        List<String> list = new ArrayList<>(parts.length);
        for (String part : parts) {
            list.add(part == null ? "" : part.trim());
        }
        return list;
    }

    public static String joinAnswers(List<String> answers) {
        if (answers == null || answers.isEmpty()) {
            return "";
        }
        return String.join(ANSWER_DELIMITER, answers);
    }

    /**
     * 用户答案是否命中标准答案（支持 | 同义词，忽略首尾空格）
     */
    public static boolean matchBlank(String userAnswer, String standard) {
        String user = userAnswer == null ? "" : userAnswer.trim();
        if (standard == null || standard.trim().isEmpty()) {
            return user.isEmpty();
        }
        String[] synonyms = standard.split("\\|");
        for (String synonym : synonyms) {
            if (user.equals(synonym.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按空比对，返回答对空数
     */
    public static int countCorrect(List<String> userAnswers, List<String> standards) {
        if (standards == null || standards.isEmpty()) {
            return 0;
        }
        int correct = 0;
        for (int i = 0; i < standards.size(); i++) {
            String user = i < userAnswers.size() ? userAnswers.get(i) : "";
            if (matchBlank(user, standards.get(i))) {
                correct++;
            }
        }
        return correct;
    }

    /**
     * 按空给分（整除）
     */
    public static int calcEarnedScore(int questionScore, int correctCount, int blankCount) {
        if (questionScore <= 0 || blankCount <= 0 || correctCount <= 0) {
            return 0;
        }
        int correct = Math.min(correctCount, blankCount);
        return questionScore * correct / blankCount;
    }
}
