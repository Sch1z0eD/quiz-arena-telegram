CREATE TABLE broadcasts (
    id            BIGSERIAL   PRIMARY KEY,
    admin_id      BIGINT      NOT NULL,
    created_at    BIGINT      NOT NULL,
    segment       VARCHAR(16) NOT NULL,
    language      VARCHAR(8),
    text          TEXT        NOT NULL,
    photo_url     TEXT,
    button_text   VARCHAR(64),
    button_url    TEXT,
    status        VARCHAR(16) NOT NULL,
    sent          INTEGER     NOT NULL DEFAULT 0,
    failed        INTEGER     NOT NULL DEFAULT 0,
    total         INTEGER     NOT NULL DEFAULT 0,
    confirm_token VARCHAR(64)
);

CREATE INDEX idx_broadcasts_created ON broadcasts (created_at DESC);
