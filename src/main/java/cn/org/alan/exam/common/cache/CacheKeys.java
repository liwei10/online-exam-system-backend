package cn.org.alan.exam.common.cache;

/**
 * Redis 业务缓存 Key 常量
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    public static final String CATEGORY_TREE = "cache:category:tree";
    public static final String CATEGORY_FIRST = "cache:category:first";
    public static final String CATEGORY_CHILDREN_PREFIX = "cache:category:children:";

    public static final String EXAM_DETAIL_PREFIX = "cache:exam:detail:";

    /** 题目内容壳（不含正确答案、用户作答） */
    public static final String QU_CONTENT_PREFIX = "cache:qu:content:";

    public static String categoryChildren(Integer parentId) {
        return CATEGORY_CHILDREN_PREFIX + parentId;
    }

    public static String examDetail(Integer examId) {
        return EXAM_DETAIL_PREFIX + examId;
    }

    public static String quContent(Integer quId) {
        return QU_CONTENT_PREFIX + quId;
    }
}
