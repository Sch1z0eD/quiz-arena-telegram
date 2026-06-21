CREATE TABLE IF NOT EXISTS users (
    id         BIGINT       PRIMARY KEY,
    name       VARCHAR(128),
    username   VARCHAR(64),
    first_seen BIGINT       NOT NULL,
    last_seen  BIGINT       NOT NULL,
    blocked    BOOLEAN      NOT NULL DEFAULT FALSE,
    banned     BOOLEAN      NOT NULL DEFAULT FALSE
);

-- Backfill the registry from existing play history. answers carries the real first/last contact.
INSERT INTO users (id, first_seen, last_seen)
SELECT user_id, MIN(answered_at), MAX(answered_at)
FROM answers
GROUP BY user_id
ON CONFLICT (id) DO NOTHING;

-- Users that exist only in settings/rating (no answers) get the migration time as a placeholder.
INSERT INTO users (id, first_seen, last_seen)
SELECT user_id, (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT, (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT
FROM user_settings
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, first_seen, last_seen)
SELECT user_id, (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT, (EXTRACT(EPOCH FROM now()) * 1000)::BIGINT
FROM user_rating
ON CONFLICT (id) DO NOTHING;
