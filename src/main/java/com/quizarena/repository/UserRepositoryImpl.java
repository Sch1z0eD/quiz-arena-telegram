package com.quizarena.repository;

import org.springframework.jdbc.core.JdbcTemplate;

public class UserRepositoryImpl implements UserRepositoryCustom {

    private static final String UPSERT_RETURNING_BANNED = """
            INSERT INTO users (id, name, username, first_seen, last_seen, blocked)
            VALUES (?, ?, ?, ?, ?, FALSE)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                username = EXCLUDED.username,
                last_seen = EXCLUDED.last_seen,
                blocked = FALSE
            RETURNING banned
            """;

    private final JdbcTemplate jdbc;

    public UserRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean touch(long id, String name, String username, long now) {
        Boolean banned = jdbc.queryForObject(UPSERT_RETURNING_BANNED, Boolean.class, id, name, username, now, now);
        return Boolean.TRUE.equals(banned);
    }
}
