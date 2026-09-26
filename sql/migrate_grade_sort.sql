-- 班级排序字段（可重复执行）
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

CALL add_column_if_missing('t_grade', 'sort',
    'int(11) NOT NULL DEFAULT 0 COMMENT ''显示排序，越小越靠前'' AFTER `code`');

-- 已有数据按 id 初始化排序（仅当全为 0 时）
UPDATE t_grade SET sort = id WHERE is_deleted = 0 AND sort = 0;

DROP PROCEDURE IF EXISTS add_column_if_missing;
