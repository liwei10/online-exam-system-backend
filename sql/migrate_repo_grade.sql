-- 存量题库与班级绑定迁移
-- 规则：将教师已开启刷题的题库，绑定到该教师加入的所有班级
-- 管理员题库不自动绑定，需在后台显式勾选班级后学生才可见

INSERT INTO t_grade_exercise (repo_id, grade_id, user_id, create_time)
SELECT r.id, ug.g_id, r.user_id, NOW()
FROM t_repo r
INNER JOIN t_user_grade ug ON ug.u_id = r.user_id AND IFNULL(ug.is_deleted, 0) = 0
WHERE r.is_deleted = 0
  AND r.is_exercise = 1
  AND NOT EXISTS (
      SELECT 1
      FROM t_grade_exercise ge
      WHERE ge.repo_id = r.id
        AND ge.grade_id = ug.g_id
  );
