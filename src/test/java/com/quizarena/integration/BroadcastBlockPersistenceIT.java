package com.quizarena.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizarena.admin.audit.AuditService;
import com.quizarena.admin.auth.VerifiedAdmin;
import com.quizarena.admin.config.AdminPanelProperties;
import com.quizarena.admin.web.BroadcastRequest;
import com.quizarena.admin.web.BroadcastService;
import com.quizarena.domain.Broadcast;
import com.quizarena.repository.BroadcastRepository;
import com.quizarena.repository.UserRepository;
import com.quizarena.service.BroadcastSender;
import com.quizarena.service.RateLimiter;
import com.quizarena.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ApiResponse;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Proves the WHOLE engine path persists blocked=true to Postgres, not just that markBlocked is invoked:
// a real BroadcastSender over a mock client throws the real 403, the engine marks the recipient, and a
// fresh JDBC read sees the committed row.
class BroadcastBlockPersistenceIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository users;
    @Autowired
    private UserService userService;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void engineCommitsBlockedToDatabaseOnReal403() throws Exception {
        long blockedUser = 7700001L;
        userService.touch(human(blockedUser, "WillBlock", "wb"));
        assertFalse(boolField(blockedUser, "blocked"), "starts unblocked");

        BroadcastRepository broadcasts = mock(BroadcastRepository.class);
        Broadcast broadcast = new Broadcast(1L, 0L, "test", null, "Hi", null, null, null, "RUNNING", 1, null);
        ReflectionTestUtils.setField(broadcast, "id", 555L);
        when(broadcasts.save(any())).thenReturn(broadcast);
        when(broadcasts.findById(555L)).thenReturn(Optional.of(broadcast));

        TelegramClient client = mock(TelegramClient.class);
        ApiResponse<?> apiResponse = new ObjectMapper().readValue(
                "{\"ok\":false,\"error_code\":403,\"description\":\"Forbidden: bot was blocked by the user\"}",
                ApiResponse.class);
        when(client.execute(any(SendMessage.class)))
                .thenThrow(new TelegramApiRequestException("Error executing sendMessage", apiResponse));

        AdminPanelProperties panel = new AdminPanelProperties(true, List.of(blockedUser), 86400, false, 0, "Dev");
        BroadcastService service = new BroadcastService(broadcasts, users, new BroadcastSender(client),
                mock(RateLimiter.class), mock(AuditService.class), panel, Runnable::run);

        service.test(new VerifiedAdmin(1, "Admin"), new BroadcastRequest("all", null, "Hi", null, null));

        assertTrue(boolField(blockedUser, "blocked"), "the engine path must commit blocked=true to the DB");
    }

    private boolean boolField(long id, String column) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT " + column + " FROM users WHERE id = ?", Boolean.class, id));
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
