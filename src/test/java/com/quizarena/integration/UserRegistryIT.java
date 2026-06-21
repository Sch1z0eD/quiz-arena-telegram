package com.quizarena.integration;

import com.quizarena.domain.AnswerRecord;
import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.GameStore;
import com.quizarena.repository.UserRepository;
import com.quizarena.service.UserNameBackfill;
import com.quizarena.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.telegram.telegrambots.meta.api.objects.User;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserRegistryIT extends AbstractIntegrationTest {

    @Autowired
    private AnswerRepository answers;
    @Autowired
    private UserRepository users;
    @Autowired
    private UserService userService;
    @Autowired
    private GameStore gameStore;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shippedBackfillSqlPopulatesRegistryAndIsIdempotent() throws Exception {
        long played = 7100001L;
        long settingsOnly = 7100003L;
        long ratingOnly = 7100004L;
        answers.save(answer(played, 1000L));
        answers.save(answer(played, 5000L));
        jdbc.update("INSERT INTO user_settings(user_id, language) VALUES (?, 'ru') ON CONFLICT (user_id) DO NOTHING", settingsOnly);
        jdbc.update("INSERT INTO user_rating(user_id, elo) VALUES (?, 1200) ON CONFLICT (user_id) DO NOTHING", ratingOnly);

        runShippedV12();

        assertEquals(1000L, longField(played, "first_seen"), "first_seen = MIN(answered_at)");
        assertEquals(5000L, longField(played, "last_seen"), "last_seen = MAX(answered_at)");
        assertTrue(exists(settingsOnly), "settings-only user is registered");
        assertTrue(exists(ratingOnly), "rating-only user is registered");

        runShippedV12(); // idempotent
        assertEquals(1000L, longField(played, "first_seen"));
        assertEquals(5000L, longField(played, "last_seen"));
    }

    @Test
    void touchInsertsThenUpdatesPreservingFirstSeenAndBanned() {
        long id = 7200001L;
        userService.touch(human(id, "Alice", "al"));
        long firstSeen = longField(id, "first_seen");
        jdbc.update("UPDATE users SET blocked = TRUE, banned = TRUE WHERE id = ?", id);

        userService.touch(human(id, "Alice Renamed", "alice2"));

        assertEquals(firstSeen, longField(id, "first_seen"), "first_seen is set once, never updated");
        assertEquals("Alice Renamed", stringField(id, "name"));
        assertEquals("alice2", stringField(id, "username"));
        assertFalse(boolField(id, "blocked"), "writing means the user has not blocked the bot");
        assertTrue(boolField(id, "banned"), "touch must not lift a ban");
    }

    @Test
    void nameBackfillFillsOnlyMissingNames() {
        long keep = 7300001L;
        long fill = 7300002L;
        userService.touch(human(keep, "KeepMe", "keep"));
        userService.touch(human(fill, "Temp", "tmp"));
        jdbc.update("UPDATE users SET name = NULL WHERE id = ?", fill);
        redis.opsForHash().put("lb:names", String.valueOf(keep), "RedisKeep");
        redis.opsForHash().put("lb:names", String.valueOf(fill), "RedisFill");

        UserNameBackfill backfill = new UserNameBackfill(gameStore, users);
        backfill.run(null);

        assertEquals("KeepMe", stringField(keep, "name"), "existing name must not be clobbered");
        assertEquals("RedisFill", stringField(fill, "name"), "null name backfilled from leaderboard hash");

        backfill.run(null); // idempotent
        assertEquals("RedisFill", stringField(fill, "name"));
    }

    private void runShippedV12() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V12__users.sql"));
        }
    }

    private long longField(long id, String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM users WHERE id = ?", Long.class, id);
    }

    private String stringField(long id, String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM users WHERE id = ?", String.class, id);
    }

    private boolean boolField(long id, String column) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT " + column + " FROM users WHERE id = ?", Boolean.class, id));
    }

    private boolean exists(long id) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private static AnswerRecord answer(long userId, long answeredAt) {
        return new AnswerRecord(1L, 1L, userId, 1L, true, 0, answeredAt, "GAME");
    }

    private static User human(long id, String firstName, String username) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getIsBot()).thenReturn(false);
        when(user.getFirstName()).thenReturn(firstName);
        when(user.getUserName()).thenReturn(username);
        return user;
    }
}
