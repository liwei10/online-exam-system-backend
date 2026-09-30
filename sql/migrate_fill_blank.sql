-- 填空题（quType=5）相关字段迁移
-- 可在已有库上重复执行：仅当列不存在时添加

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE add_column_if_missing(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition VARCHAR(512)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND COLUMN_NAME = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_column_if_missing('t_exam', 'fill_count',
    'int(11) DEFAULT 0 COMMENT ''填空题数量'' AFTER `saq_score`');
CALL add_column_if_missing('t_exam', 'fill_score',
    'int(11) DEFAULT 0 COMMENT ''填空题单题默认分 数据库存储*100，前端正常输入和展示/100'' AFTER `fill_count`');
CALL add_column_if_missing('t_exam', 'fill_need_mark',
    'tinyint(4) NOT NULL DEFAULT 0 COMMENT ''填空题是否二次人工阅卷：0否 1是'' AFTER `fill_score`');
CALL add_column_if_missing('t_exam_qu_answer', 'earned_score',
    'int(11) DEFAULT NULL COMMENT ''填空题自动实得分（按空给分）'' AFTER `ai_reason`');

-- 多空答案可能较长，扩展 answer_content
ALTER TABLE `t_exam_qu_answer`
  MODIFY COLUMN `answer_content` varchar(2000) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '答案内容 主观题/填空题(多空用|||分隔)';

DROP PROCEDURE IF EXISTS add_column_if_missing;
