-- 试题难度：1-5 星，与题目一起保存；试卷编辑时展示
-- 请先执行本脚本，再重启后端

ALTER TABLE `t_question`
    ADD COLUMN `level` INT NOT NULL DEFAULT 3 COMMENT '难度1-5，星级' AFTER `sort`;
