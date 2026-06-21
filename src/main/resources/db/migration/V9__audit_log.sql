CREATE TABLE audit_log (
    id       BIGSERIAL   PRIMARY KEY,
    ts       TIMESTAMPTZ NOT NULL,
    admin_id BIGINT      NOT NULL,
    action   VARCHAR(64) NOT NULL,
    target   VARCHAR(128),
    details  TEXT
);

CREATE INDEX idx_audit_log_ts ON audit_log (ts DESC);
