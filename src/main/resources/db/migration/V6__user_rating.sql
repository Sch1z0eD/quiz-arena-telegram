CREATE TABLE user_rating (
    user_id BIGINT PRIMARY KEY,
    elo INT NOT NULL DEFAULT 1000
);
