package com.quizarena.service;

import com.quizarena.config.DuelProperties;
import com.quizarena.config.GameProperties;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class GameSettingsTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final GameSettings settings = new GameSettings(jdbc,
            new GameProperties(20, 15, 100, 5, Duration.ofHours(1)),
            new DuelProperties(45, 20, 1, 100));

    @Test
    void updateRejectsOutOfRangeValuesWithoutTouchingTheDatabase() {
        assertThrows(IllegalArgumentException.class, () -> settings.update(snapshot(5, 2, 100, 20)));      // questionSeconds < 5
        assertThrows(IllegalArgumentException.class, () -> settings.update(snapshot(0, 15, 100, 20)));     // questionsPerGame < 1
        assertThrows(IllegalArgumentException.class, () -> settings.update(snapshot(5, 15, 99999, 20)));   // basePoints > 10000
        assertThrows(IllegalArgumentException.class, () -> settings.update(snapshot(5, 15, 100, 4)));      // lobbySeconds < 5

        verifyNoInteractions(jdbc);
    }

    private static GameSettingsSnapshot snapshot(int questionsPerGame, int questionSeconds, int basePoints, int lobbySeconds) {
        return new GameSettingsSnapshot(questionsPerGame, questionSeconds, basePoints, lobbySeconds, 45, 20, 1, 100);
    }
}
