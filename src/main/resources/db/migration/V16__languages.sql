CREATE TABLE languages (
    code    VARCHAR(8)  PRIMARY KEY,
    name    VARCHAR(64) NOT NULL,
    enabled BOOLEAN     NOT NULL DEFAULT TRUE
);

INSERT INTO languages (code, name, enabled) VALUES
    ('en', 'English', TRUE),
    ('ru', 'Русский', TRUE);
