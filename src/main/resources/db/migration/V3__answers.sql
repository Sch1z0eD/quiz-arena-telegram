CREATE TABLE answers (
    id          BIGSERIAL PRIMARY KEY,
    game_id     BIGINT  NOT NULL,
    chat_id     BIGINT  NOT NULL,
    user_id     BIGINT  NOT NULL,
    question_id BIGINT  NOT NULL,
    correct     BOOLEAN NOT NULL,
    points      INTEGER NOT NULL,
    answered_at BIGINT  NOT NULL
);

CREATE INDEX idx_answers_user ON answers (user_id);
