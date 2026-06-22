-- Single-row gameplay config; the row is seeded from application defaults on first boot (GameSettings).
CREATE TABLE game_settings (
    id                    SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    questions_per_game    INTEGER NOT NULL,
    question_seconds      INTEGER NOT NULL,
    base_points           INTEGER NOT NULL,
    lobby_seconds         INTEGER NOT NULL,
    duel_search_seconds   INTEGER NOT NULL,
    duel_question_seconds INTEGER NOT NULL,
    duel_question_count   INTEGER NOT NULL,
    duel_base_points      INTEGER NOT NULL
);
