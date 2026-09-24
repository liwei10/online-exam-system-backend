-- 全局查询性能索引（幂等：已存在则跳过）
-- 已有库升级时执行；全新安装直接用 db_exam.sql 即可

DELIMITER $$

DROP PROCEDURE IF EXISTS add_index_if_missing $$
CREATE PROCEDURE add_index_if_missing(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(255)
)
BEGIN
    DECLARE idx_count INT DEFAULT 0;
    SELECT COUNT(1) INTO idx_count
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND INDEX_NAME = p_index;
    IF idx_count = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_columns, ')');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

CALL add_index_if_missing('t_user_grade', 'idx_ug_g_id', 'g_id');
CALL add_index_if_missing('t_user', 'idx_user_grade_del', 'grade_id, is_deleted');
CALL add_index_if_missing('t_user', 'idx_user_role', 'role_id');
CALL add_index_if_missing('t_grade', 'idx_grade_user_id', 'user_id');
CALL add_index_if_missing('t_exam_grade', 'idx_eg_exam_id', 'exam_id');
CALL add_index_if_missing('t_exam_grade', 'idx_eg_grade_id', 'grade_id');
CALL add_index_if_missing('t_exam_grade', 'idx_eg_exam_grade', 'exam_id, grade_id');
CALL add_index_if_missing('t_exam', 'idx_exam_user_del', 'user_id, is_deleted');
CALL add_index_if_missing('t_user_exams_score', 'idx_ues_exam_mark', 'exam_id, whether_mark');
CALL add_index_if_missing('t_user_exams_score', 'idx_ues_exam_state', 'exam_id, state');
CALL add_index_if_missing('t_user_exams_score', 'idx_ues_user_state', 'user_id, state');
CALL add_index_if_missing('t_exam_qu_answer', 'idx_eqa_exam_qid', 'exam_id, question_id');
CALL add_index_if_missing('t_exam_qu_answer', 'idx_eqa_exam_user_type', 'exam_id, user_id, question_type');
CALL add_index_if_missing('t_exam_question', 'idx_eq_exam_type', 'exam_id, type');
CALL add_index_if_missing('t_exam_question', 'idx_eq_exam_qid', 'exam_id, question_id');
CALL add_index_if_missing('t_user_book', 'idx_ub_user_exam', 'user_id, exam_id');
CALL add_index_if_missing('t_user_exercise_record', 'idx_uer_user_repo', 'user_id, repo_id');
CALL add_index_if_missing('t_user_daily_login_duration', 'idx_udld_user_date', 'user_id, login_date');
CALL add_index_if_missing('t_repo', 'idx_repo_user_del', 'user_id, is_deleted');
CALL add_index_if_missing('t_repo', 'idx_repo_category', 'category_id');
CALL add_index_if_missing('t_exam_repo', 'idx_er_exam_id', 'exam_id');
CALL add_index_if_missing('t_exam_repo', 'idx_er_repo_id', 'repo_id');
CALL add_index_if_missing('t_option', 'idx_option_qu_id', 'qu_id');
CALL add_index_if_missing('t_option', 'idx_option_qu_right', 'qu_id, is_right');
CALL add_index_if_missing('t_question', 'idx_qu_repo_del', 'repo_id, is_deleted');
CALL add_index_if_missing('t_question', 'idx_qu_repo_type_del', 'repo_id, qu_type, is_deleted');
CALL add_index_if_missing('t_notice_grade', 'idx_ng_grade', 'grade_id');
CALL add_index_if_missing('t_notice_grade', 'idx_ng_notice', 'notice_id');
CALL add_index_if_missing('t_certificate_user', 'idx_cu_user', 'user_id');
CALL add_index_if_missing('t_certificate_user', 'idx_cu_exam', 'exam_id');
CALL add_index_if_missing('t_discussion', 'idx_disc_grade', 'grade_id');
CALL add_index_if_missing('t_discussion', 'idx_disc_user', 'user_id');
CALL add_index_if_missing('t_reply', 'idx_reply_disc_parent', 'discussion_id, parent_id');
CALL add_index_if_missing('t_reply', 'idx_reply_parent', 'parent_id');
CALL add_index_if_missing('t_like', 'idx_like_reply', 'reply_id');
CALL add_index_if_missing('t_exercise_record', 'idx_er_user_repo', 'user_id, repo_id');
CALL add_index_if_missing('t_manual_score', 'idx_ms_answer', 'exam_qu_answer_id');
CALL add_index_if_missing('t_grade_exercise', 'idx_grade_id', 'grade_id');

DROP PROCEDURE IF EXISTS add_index_if_missing;
