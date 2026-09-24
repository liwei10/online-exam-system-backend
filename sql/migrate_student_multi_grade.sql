-- 学生多班级：把历史单班级同步到 t_user_grade
INSERT INTO t_user_grade (u_id, g_id, is_deleted)
SELECT u.id, u.grade_id, 0
FROM t_user u
WHERE u.grade_id IS NOT NULL
  AND u.is_deleted = 0
  AND u.role_id = 1
  AND NOT EXISTS (
      SELECT 1 FROM t_user_grade ug
      WHERE ug.u_id = u.id AND ug.g_id = u.grade_id AND (ug.is_deleted = 0 OR ug.is_deleted IS NULL)
  );
