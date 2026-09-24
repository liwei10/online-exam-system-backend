-- 试题排序字段：刷题中心按 sort 升序展示，可在试题管理中调整顺序
-- 请先执行本脚本，再重启后端

ALTER TABLE `t_question`
    ADD COLUMN `sort` INT NOT NULL DEFAULT 0 COMMENT '题库内排序，越小越靠前' AFTER `repo_id`;

-- 兼容 MySQL 5.7：按题库分组，用变量按 create_time 初始化 sort
SET @rn := 0;
SET @prev_repo := NULL;
UPDATE t_question q
JOIN (
  SELECT id,
         @rn := IF(@prev_repo = repo_id, @rn + 1, 1) AS rn,
         @prev_repo := repo_id AS repo_id
  FROM t_question
  WHERE is_deleted = 0
  ORDER BY repo_id ASC, create_time ASC, id ASC
) t ON q.id = t.id
SET q.sort = t.rn;
