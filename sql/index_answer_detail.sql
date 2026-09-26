-- 阅卷/答卷详情查询加速（可选执行；若索引已存在会报错，可忽略）
-- 将「每题循环查库」优化后，配合下列索引效果更好

CREATE INDEX idx_exam_question_exam_sort ON t_exam_question (exam_id, sort);
CREATE INDEX idx_option_qu_id ON t_option (qu_id);
CREATE INDEX idx_exam_qu_answer_exam_user ON t_exam_qu_answer (exam_id, user_id);
CREATE INDEX idx_exam_qu_answer_exam_user_qu ON t_exam_qu_answer (exam_id, user_id, question_id);
