package com.quizarena.service;

import com.quizarena.config.DuelProperties;
import com.quizarena.config.GameProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Admin-tunable gameplay parameters. Backed by a single row, cached in an {@link AtomicReference} so the
 * per-question read path never touches the database; the snapshot is replaced atomically when an admin saves.
 * Defaults come from {@link GameProperties}/{@link DuelProperties} and seed the row on first boot.
 */
@Service
public class GameSettings {

    private static final String SELECT = """
            SELECT questions_per_game, question_seconds, base_points, lobby_seconds,
                   duel_search_seconds, duel_question_seconds, duel_question_count, duel_base_points
            FROM game_settings WHERE id = 1""";

    private final JdbcTemplate jdbc;
    private final GameProperties gameDefaults;
    private final DuelProperties duelDefaults;
    private final AtomicReference<GameSettingsSnapshot> current = new AtomicReference<>();

    public GameSettings(JdbcTemplate jdbc, GameProperties gameDefaults, DuelProperties duelDefaults) {
        this.jdbc = jdbc;
        this.gameDefaults = gameDefaults;
        this.duelDefaults = duelDefaults;
    }

    @PostConstruct
    void load() {
        GameSettingsSnapshot snapshot = read();
        if (snapshot == null) {
            snapshot = defaults();
            insert(snapshot);
        }
        current.set(snapshot);
    }

    public GameSettingsSnapshot snapshot() {
        return current.get();
    }

    public int questionsPerGame() {
        return current.get().questionsPerGame();
    }

    public int questionSeconds() {
        return current.get().questionSeconds();
    }

    public int basePoints() {
        return current.get().basePoints();
    }

    public int lobbySeconds() {
        return current.get().lobbySeconds();
    }

    public int duelSearchSeconds() {
        return current.get().duelSearchSeconds();
    }

    public int duelQuestionSeconds() {
        return current.get().duelQuestionSeconds();
    }

    public int duelQuestionCount() {
        return current.get().duelQuestionCount();
    }

    public int duelBasePoints() {
        return current.get().duelBasePoints();
    }

    public GameSettingsSnapshot update(GameSettingsSnapshot values) {
        validate(values);
        jdbc.update("""
                UPDATE game_settings SET questions_per_game = ?, question_seconds = ?, base_points = ?,
                       lobby_seconds = ?, duel_search_seconds = ?, duel_question_seconds = ?,
                       duel_question_count = ?, duel_base_points = ? WHERE id = 1""",
                values.questionsPerGame(), values.questionSeconds(), values.basePoints(), values.lobbySeconds(),
                values.duelSearchSeconds(), values.duelQuestionSeconds(), values.duelQuestionCount(),
                values.duelBasePoints());
        current.set(values);
        return values;
    }

    static void validate(GameSettingsSnapshot s) {
        range("questionsPerGame", s.questionsPerGame(), 1, 50);
        range("questionSeconds", s.questionSeconds(), 5, 120);
        range("basePoints", s.basePoints(), 10, 10000);
        range("lobbySeconds", s.lobbySeconds(), 5, 300);
        range("duelSearchSeconds", s.duelSearchSeconds(), 10, 300);
        range("duelQuestionSeconds", s.duelQuestionSeconds(), 5, 120);
        range("duelQuestionCount", s.duelQuestionCount(), 1, 50);
        range("duelBasePoints", s.duelBasePoints(), 10, 10000);
    }

    private static void range(String name, int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
        }
    }

    private GameSettingsSnapshot read() {
        return jdbc.query(SELECT, rs -> rs.next() ? map(rs) : null);
    }

    private static GameSettingsSnapshot map(ResultSet rs) throws SQLException {
        return new GameSettingsSnapshot(
                rs.getInt("questions_per_game"), rs.getInt("question_seconds"), rs.getInt("base_points"),
                rs.getInt("lobby_seconds"), rs.getInt("duel_search_seconds"), rs.getInt("duel_question_seconds"),
                rs.getInt("duel_question_count"), rs.getInt("duel_base_points"));
    }

    private GameSettingsSnapshot defaults() {
        return new GameSettingsSnapshot(
                gameDefaults.questionsPerGame(), gameDefaults.questionSeconds(), gameDefaults.basePoints(),
                gameDefaults.lobbySeconds(), duelDefaults.searchSeconds(), duelDefaults.questionSeconds(),
                duelDefaults.questionCount(), duelDefaults.basePoints());
    }

    private void insert(GameSettingsSnapshot s) {
        jdbc.update("""
                INSERT INTO game_settings (id, questions_per_game, question_seconds, base_points, lobby_seconds,
                       duel_search_seconds, duel_question_seconds, duel_question_count, duel_base_points)
                VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING""",
                s.questionsPerGame(), s.questionSeconds(), s.basePoints(), s.lobbySeconds(),
                s.duelSearchSeconds(), s.duelQuestionSeconds(), s.duelQuestionCount(), s.duelBasePoints());
    }
}
