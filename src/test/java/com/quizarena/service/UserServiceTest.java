package com.quizarena.service;

import com.quizarena.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final UserService service = new UserService(users);

    @Test
    void touchRegistersHumanAndReturnsBannedFlag() {
        when(users.touch(eq(7L), eq("Alice"), eq("al"), anyLong())).thenReturn(true);
        assertTrue(service.touch(human(7L, "Alice", "al")), "touch returns the banned flag from the upsert");
        verify(users).touch(eq(7L), eq("Alice"), eq("al"), anyLong());
    }

    @Test
    void touchReturnsFalseForNonBanned() {
        when(users.touch(eq(7L), eq("Alice"), eq("al"), anyLong())).thenReturn(false);
        assertFalse(service.touch(human(7L, "Alice", "al")));
    }

    @Test
    void touchSkipsNullFrom() {
        service.touch(null);
        verify(users, never()).touch(anyLong(), anyString(), anyString(), anyLong());
    }

    @Test
    void touchSkipsBots() {
        User bot = human(99L, "BotFace", "botface");
        when(bot.getIsBot()).thenReturn(true);
        service.touch(bot);
        verify(users, never()).touch(anyLong(), anyString(), anyString(), anyLong());
    }

    @Test
    void touchFailsOpenOnRepositoryFailure() {
        doThrow(new RuntimeException("db down")).when(users).touch(anyLong(), anyString(), anyString(), anyLong());
        assertFalse(service.touch(human(7L, "Alice", "al")), "a DB failure must not block everyone (fail-open)");
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
