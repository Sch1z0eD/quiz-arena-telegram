package com.quizarena.admin.web;

import com.quizarena.admin.audit.AuditService;
import com.quizarena.admin.auth.VerifiedAdmin;
import com.quizarena.admin.config.AdminPanelProperties;
import com.quizarena.domain.Broadcast;
import com.quizarena.repository.BroadcastRepository;
import com.quizarena.repository.UserRepository;
import com.quizarena.service.BroadcastSender;
import com.quizarena.service.RateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ApiResponse;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Collections;
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
        List<List<BroadcastRequest.Button>> buttons = List.of(List.of(new BroadcastRequest.Button("Open", "ftp://x")));
        assertThrows(IllegalArgumentException.class, () -> service.dryRun(ADMIN, request("all", null, "hi", null, buttons)));
    }

    @Test
    void dryRunRejectsButtonLimitsBlanksAndEmptyRows() {
        BroadcastRequest.Button ok = new BroadcastRequest.Button("B", "https://b");
        assertThrows(IllegalArgumentException.class, () ->
                service.dryRun(ADMIN, request("all", null, "hi", null, Collections.nCopies(11, List.of(ok)))));
        assertThrows(IllegalArgumentException.class, () ->
                service.dryRun(ADMIN, request("all", null, "hi", null, List.of(Collections.nCopies(9, ok)))));
        assertThrows(IllegalArgumentException.class, () ->
                service.dryRun(ADMIN, request("all", null, "hi", null, List.of(List.of(new BroadcastRequest.Button("  ", "https://b"))))));
        assertThrows(IllegalArgumentException.class, () ->
                service.dryRun(ADMIN, request("all", null, "hi", null, List.of(List.<BroadcastRequest.Button>of()))));
    }

    @Test
    void uploadPhotoRejectsNonImageAndOversized() throws Exception {
        MockMultipartFile notImage = new MockMultipartFile("file", "x.txt", "text/plain", new byte[]{1, 2, 3});
        assertThrows(IllegalArgumentException.class, () -> service.uploadPhoto(ADMIN, notImage));

        MockMultipartFile oversized = new MockMultipartFile("file", "big.jpg", "image/jpeg", new byte[6 * 1024 * 1024]);
        assertThrows(IllegalArgumentException.class, () -> service.uploadPhoto(ADMIN, oversized));

        verify(sender, never()).uploadPhoto(anyLong(), any(), any());
    }

    @Test
    void dryRunAcceptsMultipleButtonRows() {
        when(users.countRecipients("")).thenReturn(5L);
        when(broadcasts.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), 3L));
        List<List<BroadcastRequest.Button>> rows = List.of(
                List.of(new BroadcastRequest.Button("A", "https://a"), new BroadcastRequest.Button("B", "https://b")),
                List.of(new BroadcastRequest.Button("C", "https://c")));

        DryRunResponse response = service.dryRun(ADMIN, request("all", null, "hi", null, rows));

        assertEquals(3L, response.id());
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
    void engineMarksBlockedWhenRealClientReports403() throws Exception {
        TelegramClient telegramClient = mock(TelegramClient.class);
        BroadcastSender realSender = new BroadcastSender(telegramClient);
        BroadcastService service =
                new BroadcastService(broadcasts, users, realSender, rateLimiter, audit, panel, synchronous);

        Broadcast b = withId(broadcast("test", null, "RUNNING"), 9L);
        when(broadcasts.save(any())).thenReturn(b);
        when(broadcasts.findById(9L)).thenReturn(Optional.of(b));

        ApiResponse<?> apiResponse = new ObjectMapper().readValue(
                "{\"ok\":false,\"error_code\":403,\"description\":\"Forbidden: bot was blocked by the user\"}",
                ApiResponse.class);
        TelegramApiRequestException blocked = new TelegramApiRequestException("Error executing sendMessage", apiResponse);
        when(telegramClient.execute(any(SendMessage.class))).thenThrow(blocked).thenReturn(null);

        service.test(ADMIN, request("all", null, "Hi", null, null));

        verify(users).markBlocked(111L);
        verify(broadcasts).finish(9L, "DONE", 1, 1); // one failure does not abort the batch
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
                                            List<List<BroadcastRequest.Button>> buttons) {
        return new BroadcastRequest(segment, language, text, photoUrl, buttons);
    }

    private static Broadcast broadcast(String segment, String language, String status) {
        return new Broadcast(1L, 0L, segment, language, "Hi", null, null, status, 0, null);
    }

    private static Broadcast withId(Broadcast broadcast, long id) {
        ReflectionTestUtils.setField(broadcast, "id", id);
        return broadcast;
    }
}
