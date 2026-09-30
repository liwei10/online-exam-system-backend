-- 清理历史上逻辑删除的试卷及其关联脏数据（一次性）
-- 全新安装无需执行；已有库若曾用逻辑删除可执行本脚本

DELETE ms FROM t_manual_score ms
INNER JOIN t_exam_qu_answer eqa ON ms.exam_qu_answer_id = eqa.id
INNER JOIN t_exam e ON e.id = eqa.exam_id
WHERE e.is_deleted = 1;

DELETE eqa FROM t_exam_qu_answer eqa
INNER JOIN t_exam e ON e.id = eqa.exam_id
WHERE e.is_deleted = 1;

DELETE ues FROM t_user_exams_score ues
INNER JOIN t_exam e ON e.id = ues.exam_id
WHERE e.is_deleted = 1;

DELETE ub FROM t_user_book ub
INNER JOIN t_exam e ON e.id = ub.exam_id
WHERE e.is_deleted = 1;

DELETE cu FROM t_certificate_user cu
INNER JOIN t_exam e ON e.id = cu.exam_id
WHERE e.is_deleted = 1;

DELETE eq FROM t_exam_question eq
INNER JOIN t_exam e ON e.id = eq.exam_id
WHERE e.is_deleted = 1;

DELETE eg FROM t_exam_grade eg
INNER JOIN t_exam e ON e.id = eg.exam_id
WHERE e.is_deleted = 1;

DELETE er FROM t_exam_repo er
INNER JOIN t_exam e ON e.id = er.exam_id
WHERE e.is_deleted = 1;

DELETE FROM t_exam WHERE is_deleted = 1;
