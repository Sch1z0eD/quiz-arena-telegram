package com.quizarena.service;

import com.quizarena.domain.Language;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Single source of truth for which languages the bot and admin panel offer. The enabled set is read often
 * (every language picker, every locale resolution) and changes rarely, so it is cached in an
 * {@link AtomicReference}. Translation strings stay in resource bundles; only the available-language list
 * lives in the database. {@link #reload()} is public so a future admin mutation can refresh the cache.
 */
@Service
public class LanguageRegistry {

    private static final String SELECT_ENABLED = "SELECT code, name FROM languages WHERE enabled = TRUE ORDER BY code";

    private final JdbcTemplate jdbc;
    private final AtomicReference<List<Language>> enabled = new AtomicReference<>(List.of());

    public LanguageRegistry(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void reload() {
        enabled.set(List.copyOf(jdbc.query(SELECT_ENABLED,
                (rs, row) -> new Language(rs.getString("code"), rs.getString("name")))));
    }

    public List<Language> enabled() {
        return enabled.get();
    }

    public boolean isEnabled(String code) {
        return code != null && enabled.get().stream().anyMatch(language -> language.code().equals(code));
    }
}
