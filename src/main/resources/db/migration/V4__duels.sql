ALTER TABLE answers ADD COLUMN mode VARCHAR(8) NOT NULL DEFAULT 'GAME';

CREATE TABLE duels (
    id          BIGINT PRIMARY KEY,
    user_a      BIGINT  NOT NULL,
    user_b      BIGINT  NOT NULL,
    score_a     INTEGER NOT NULL,
    score_b     INTEGER NOT NULL,
    winner_id   BIGINT,
    category    VARCHAR(64),
    difficulty  VARCHAR(16),
    finished_at BIGINT  NOT NULL
);

CREATE INDEX idx_duels_user_a ON duels (user_a);
CREATE INDEX idx_duels_user_b ON duels (user_b);
