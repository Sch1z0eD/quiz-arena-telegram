ALTER TABLE questions ADD COLUMN category      VARCHAR(64);
ALTER TABLE questions ADD COLUMN difficulty    VARCHAR(16);
ALTER TABLE questions ADD COLUMN language      VARCHAR(8) NOT NULL DEFAULT 'ru';
ALTER TABLE questions ADD COLUMN question_hash VARCHAR(32);

UPDATE questions SET question_hash = md5(text) WHERE question_hash IS NULL;

ALTER TABLE questions ALTER COLUMN question_hash SET NOT NULL;
ALTER TABLE questions ADD CONSTRAINT uq_questions_hash UNIQUE (question_hash);
