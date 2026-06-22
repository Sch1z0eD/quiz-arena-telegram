package com.quizarena.admin.web;

import com.quizarena.admin.audit.AuditService;
import com.quizarena.admin.auth.VerifiedAdmin;
import com.quizarena.admin.config.AdminPanelProperties;
import com.quizarena.domain.Broadcast;
import com.quizarena.repository.BroadcastRepository;
import com.quizarena.repository.UserRepository;
import com.quizarena.service.BroadcastSender;
import com.quizarena.service.RateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BroadcastServiceTest {

    private static final VerifiedAdmin ADMIN = new VerifiedAdmin(1, "Admin");

    private final BroadcastRepository broadcasts = mock(BroadcastRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final BroadcastSender sender = mock(BroadcastSender.class);
    private final RateLimiter rateLimiter = mock(RateLimiter.class);
    private final AuditService audit = mock(AuditService.class);
    private final AdminPanelProperties panel =
            new AdminPanelProperties(true, List.of(111L, 222L), 86400, false, 0, "Dev");
    private final Executor synchronous = Runnable::run;
    private final BroadcastService service =
            new BroadcastService(broadcasts, users, sender, rateLimiter, audit, panel, synchronous);

    @Test
    void dryRunSavesDraftWithTokenAndCountWithoutSending() throws Exception {
        when(users.countRecipients("")).thenReturn(42L);
        when(broadcasts.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), 7L));

        DryRunResponse response = service.dryRun(ADMIN, request("all", null, "Hello", null, null));

        assertEquals(7L, response.id());
        assertEquals(42L, response.total());
        assertNotNull(response.token());
        verify(sender, never()).send(anyLong(), any());
    }

    @Test
    void dryRunRejectsBadSegmentMissingLanguageAndEmptyText() {
        assertThrows(IllegalArgumentException.class, () -> service.dryRun(ADMIN, request("nope", null, "x", null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.dryRun(ADMIN, request("by-language", null, "x", null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.dryRun(ADMIN, request("all", null, "  ", null, null)));
    }

    @Test
    void dryRunRejectsBadButtonUrl() {
        BroadcastRequest.Button button = new BroadcastRequest.Button("Open", "ftp://x");
        assertThrows(IllegalArgumentException.class, () -> service.dryRun(ADMIN, request("all", null, "hi", null, button)));
    }

    @Test
    void testRunsEngineOverAllowlistAndCounts() throws Exception {
        Broadcast b = withId(broadcast("test", null, "RUNNING"), 9L);
        when(broadcasts.save(any())).thenReturn(b);
        when(broadcasts.findById(9L)).thenReturn(Optional.of(b));

        service.test(ADMIN, request("all", null, "Hi", null, null));

        verify(sender, times(2)).send(anyLong(), eq(b));
        verify(broadcasts).finish(9L, "DONE", 2, 0);
        verify(audit).record(eq(ADMIN), eq("broadcast.started"), eq("9"), anyString());
        verify(audit).record(any(), eq("broadcast.completed"), eq("9"), anyString());
    }

    @Test
    void engineMarksBlockedOn403AndKeepsGoing() throws Exception {
        Broadcast b = withId(broadcast("test", null, "RUNNING"), 9L);
        when(broadcasts.save(any())).thenReturn(b);
        when(broadcasts.findById(9L)).thenReturn(Optional.of(b));
        TelegramApiRequestException blocked = mock(TelegramApiRequestException.class);
        when(blocked.getErrorCode()).thenReturn(403);
        doThrow(blocked).when(sender).send(111L, b);

        service.test(ADMIN, request("all", null, "Hi", null, null));

        verify(users).markBlocked(111L);
        verify(sender).send(222L, b); // one failure does not abort the batch
        verify(broadcasts).finish(9L, "DONE", 1, 1);
    }

    @Test
    void startRunsWhenTokenFlipsDraft() throws Exception {
        when(broadcasts.existsByStatus("RUNNING")).thenReturn(false);
        Broadcast b = withId(broadcast("all", null, "DRAFT"), 5L);
        when(broadcasts.findById(5L)).thenReturn(Optional.of(b));
        when(users.findRecipientIds("")).thenReturn(List.of(111L));
        when(broadcasts.startIfDraft(5L, "tok", 1)).thenReturn(1);

        service.start(ADMIN, 5L, "tok");

        verify(sender).send(111L, b);
        verify(broadcasts).finish(5L, "DONE", 1, 0);
    }

    @Test
    void startRejectsBadTokenAndSendsNothing() throws Exception {
        when(broadcasts.existsByStatus("RUNNING")).thenReturn(false);
        when(broadcasts.findById(5L)).thenReturn(Optional.of(withId(broadcast("all", null, "DRAFT"), 5L)));
        when(users.findRecipientIds("")).thenReturn(List.of(111L));
        when(broadcasts.startIfDraft(5L, "bad", 1)).thenReturn(0);

        assertThrows(Exception.class, () -> service.start(ADMIN, 5L, "bad"));
        verify(sender, never()).send(anyLong(), any());
    }

    @Test
    void startRejectedWhenAnotherBroadcastRunning() throws Exception {
        when(broadcasts.existsByStatus("RUNNING")).thenReturn(true);
        assertThrows(Exception.class, () -> service.start(ADMIN, 5L, "tok"));
        verify(sender, never()).send(anyLong(), any());
    }

    @Test
    void abortStopsEngineBeforeAnySend() throws Exception {
        Broadcast b = withId(broadcast("all", null, "RUNNING"), 3L);
        when(broadcasts.findById(3L)).thenReturn(Optional.of(b));

        service.abort(ADMIN, 3L);
        service.runEngine(3L, List.of(111L, 222L));

        verify(sender, never()).send(anyLong(), any());
        verify(broadcasts).finish(3L, "ABORTED", 0, 0);
        verify(audit).record(eq(ADMIN), eq("broadcast.aborted"), eq("3"), any());
        verify(audit, never()).record(any(), eq("broadcast.completed"), anyString(), any());
    }

    private static BroadcastRequest request(String segment, String language, String text, String photoUrl,
                                            BroadcastRequest.Button button) {
        return new BroadcastRequest(segment, language, text, photoUrl, button);
    }

    private static Broadcast broadcast(String segment, String language, String status) {
        return new Broadcast(1L, 0L, segment, language, "Hi", null, null, null, status, 0, null);
    }

    private static Broadcast withId(Broadcast broadcast, long id) {
        ReflectionTestUtils.setField(broadcast, "id", id);
        return broadcast;
    }
}
