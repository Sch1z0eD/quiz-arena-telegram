CREATE TABLE questions (
    id             BIGSERIAL PRIMARY KEY,
    text           TEXT      NOT NULL,
    option_a       TEXT      NOT NULL,
    option_b       TEXT      NOT NULL,
    option_c       TEXT      NOT NULL,
    option_d       TEXT      NOT NULL,
    correct_option INTEGER   NOT NULL CHECK (correct_option BETWEEN 0 AND 3)
);

INSERT INTO questions (text, option_a, option_b, option_c, option_d, correct_option) VALUES
('Столица Австралии?',
 'Сидней', 'Мельбурн', 'Канберра', 'Брисбен', 2),

('Какой язык программирования создал Джеймс Гослинг?',
 'C++', 'Java', 'Python', 'Go', 1),

('Сколько байт в одном килобайте (KiB)?',
 '1000', '1024', '512', '2048', 1),

('В каком году был основан GitHub?',
 '2005', '2006', '2008', '2010', 2),

('Какой протокол используется для безопасной передачи веб-страниц?',
 'FTP', 'HTTP', 'SMTP', 'HTTPS', 3);
