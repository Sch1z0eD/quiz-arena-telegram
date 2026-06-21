CREATE TABLE categories (
    id   BIGSERIAL   PRIMARY KEY,
    slug VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE category_translations (
    category_id BIGINT       NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    language    VARCHAR(8)   NOT NULL,
    name        VARCHAR(128) NOT NULL,
    PRIMARY KEY (category_id, language)
);

INSERT INTO categories (slug) VALUES
    ('general'), ('science'), ('computers'), ('geography'), ('history'),
    ('sports'), ('film'), ('music'), ('games'), ('mythology');

-- Seed names from the current message bundle so existing categories keep their exact RU/EN names.
INSERT INTO category_translations (category_id, language, name)
SELECT c.id, v.language, v.name
FROM categories c
JOIN (VALUES
    ('general',   'en', 'General knowledge'), ('general',   'ru', 'Общие знания'),
    ('science',   'en', 'Science'),           ('science',   'ru', 'Наука'),
    ('computers', 'en', 'Computers'),         ('computers', 'ru', 'Компьютеры'),
    ('geography', 'en', 'Geography'),         ('geography', 'ru', 'География'),
    ('history',   'en', 'History'),           ('history',   'ru', 'История'),
    ('sports',    'en', 'Sports'),            ('sports',    'ru', 'Спорт'),
    ('film',      'en', 'Film'),              ('film',      'ru', 'Кино'),
    ('music',     'en', 'Music'),             ('music',     'ru', 'Музыка'),
    ('games',     'en', 'Video games'),       ('games',     'ru', 'Видеоигры'),
    ('mythology', 'en', 'Mythology'),         ('mythology', 'ru', 'Мифология')
) AS v (slug, language, name) ON c.slug = v.slug;
