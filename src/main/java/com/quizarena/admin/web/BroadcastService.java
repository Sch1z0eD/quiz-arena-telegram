package com.quizarena.admin.web;

import com.quizarena.admin.audit.AuditService;
import com.quizarena.admin.auth.VerifiedAdmin;
import com.quizarena.admin.config.AdminPanelProperties;
import com.quizarena.admin.web.BroadcastRequest.Button;
import com.quizarena.domain.Broadcast;
import com.quizarena.repository.BroadcastRepository;
import com.quizarena.repository.UserRepository;
import com.quizarena.service.BroadcastSender;
import com.quizarena.service.RateLimiter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Service
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class BroadcastService {

    private static final Logger log = LoggerFactory.getLogger(BroadcastService.class);

    private static final String SEGMENT_ALL = "all";
    private static final String SEGMENT_BY_LANGUAGE = "by-language";
    private static final String SEGMENT_TEST = "test";
    private static final String DRAFT = "DRAFT";
    private static final String RUNNING = "RUNNING";
    private static final String DONE = "DONE";
    private static final String ABORTED = "ABORTED";
    private static final String BUCKET = "broadcast:bucket";
    private static final int PROGRESS_FLUSH = 25;
    private static final int MAX_TEXT = 4096;
    private static final int MAX_CAPTION = 1024;

    private final BroadcastRepository broadcasts;
    private final UserRepository users;
    private final BroadcastSender sender;
    private final RateLimiter rateLimiter;
    private final AuditService audit;
    private final AdminPanelProperties panel;
    private final Executor executor;

    private final Set<Long> aborted = ConcurrentHashMap.newKeySet();

    public BroadcastService(BroadcastRepository broadcasts, UserRepository users, BroadcastSender sender,
                            RateLimiter rateLimiter, AuditService audit, AdminPanelProperties panel, Executor executor) {
        this.broadcasts = broadcasts;
        this.users = users;
        this.sender = sender;
        this.rateLimiter = rateLimiter;
        this.audit = audit;
        this.panel = panel;
        this.executor = executor;
    }

    // A broadcast cut off by a restart is never auto-resumed; the admin decides, so a restart cannot double-send.
    @PostConstruct
    void interruptRunningOnStartup() {
        int interrupted = broadcasts.markRunningInterrupted();
        if (interrupted > 0) {
            log.warn("Marked {} running broadcast(s) INTERRUPTED on startup; not auto-resumed", interrupted);
        }
    }

    public DryRunResponse dryRun(VerifiedAdmin admin, BroadcastRequest request) {
        String segment = request.segment();
        if (!SEGMENT_ALL.equals(segment) && !SEGMENT_BY_LANGUAGE.equals(segment)) {
            throw new IllegalArgumentException("segment must be 'all' or 'by-language'");
        }
        String language = SEGMENT_BY_LANGUAGE.equals(segment) ? requireLanguage(request) : null;
        validateContent(request);
        long total = users.countRecipients(language == null ? "" : language);
        String token = UUID.randomUUID().toString().replace("-", "");
        Broadcast broadcast = broadcasts.save(draft(admin, segment, language, request, token, (int) total));
        return new DryRunResponse(broadcast.getId(), total, token);
    }

    public BroadcastSummary test(VerifiedAdmin admin, BroadcastRequest request) {
        validateContent(request);
        List<Long> recipients = panel.admins();
        Broadcast broadcast = broadcasts.save(new Broadcast(admin.id(), System.currentTimeMillis(), SEGMENT_TEST, null,
                request.text().trim(), trimToNull(request.photoUrl()), buttonText(request), buttonUrl(request),
                RUNNING, recipients.size(), null));
        audit.record(admin, "broadcast.started", String.valueOf(broadcast.getId()), "segment=test total=" + recipients.size());
        launch(broadcast.getId(), recipients);
        return toSummary(broadcast);
    }

    public void start(VerifiedAdmin admin, long id, String token) {
        if (broadcasts.existsByStatus(RUNNING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "another broadcast is already running");
        }
        Broadcast broadcast = broadcasts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Long> recipients = users.findRecipientIds(broadcast.getLanguage() == null ? "" : broadcast.getLanguage());
        if (broadcasts.startIfDraft(id, token, recipients.size()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "invalid confirm token or broadcast already started");
        }
        audit.record(admin, "broadcast.started", String.valueOf(id),
                "segment=" + broadcast.getSegment() + " total=" + recipients.size());
        launch(id, recipients);
    }

    public void abort(VerifiedAdmin admin, long id) {
        broadcasts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        aborted.add(id);
        audit.record(admin, "broadcast.aborted", String.valueOf(id), null);
    }

    public PageResponse<BroadcastSummary> history(int page, int size) {
        Page<Broadcast> result = broadcasts.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return new PageResponse<>(result.getContent().stream().map(BroadcastService::toSummary).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public Optional<BroadcastSummary> status(long id) {
        return broadcasts.findById(id).map(BroadcastService::toSummary);
    }

    private void launch(long id, List<Long> recipients) {
        executor.execute(() -> runEngine(id, recipients));
    }

    // Single engine for both test and full: cursor + throttle + per-recipient try/catch + abort + counters.
    void runEngine(long id, List<Long> recipients) {
        Broadcast broadcast = broadcasts.findById(id).orElse(null);
        if (broadcast == null) {
            return;
        }
        int sent = 0;
        int failed = 0;
        String finalStatus = DONE;
        try {
            for (Long userId : recipients) {
                if (aborted.contains(id)) {
                    finalStatus = ABORTED;
                    break;
                }
                rateLimiter.acquire(BUCKET);
                try {
                    sender.send(userId, broadcast);
                    sent++;
                } catch (TelegramApiException e) {
                    failed++;
                    logSendFailure(id, userId, e);
                    if (isBotBlocked(e)) {
                        markBlocked(userId);
                    }
                } catch (RuntimeException e) {
                    failed++;
                    logSendFailure(id, userId, e);
                }
                if ((sent + failed) % PROGRESS_FLUSH == 0) {
                    broadcasts.updateProgress(id, sent, failed);
                }
            }
        } finally {
            broadcasts.finish(id, finalStatus, sent, failed);
            aborted.remove(id);
        }
        if (DONE.equals(finalStatus)) {
            audit.record(new VerifiedAdmin(broadcast.getAdminId(), ""), "broadcast.completed",
                    String.valueOf(id), "sent=" + sent + " failed=" + failed);
        }
    }

    private void markBlocked(long userId) {
        try {
            users.markBlocked(userId);
        } catch (RuntimeException e) {
            log.warn("Failed to mark user {} blocked after 403", userId, e);
        }
    }

    private static void logSendFailure(long id, long userId, Exception e) {
        Integer errorCode = e instanceof TelegramApiRequestException request ? request.getErrorCode() : null;
        log.warn("Broadcast {} send to {} failed: type={} errorCode={} message={}",
                id, userId, e.getClass().getSimpleName(), errorCode, e.getMessage());
    }

    // getErrorCode() is nullable and not populated on every error path, so fall back to the textual marker
    // Telegram returns for a stopped bot ("Forbidden: bot was blocked by the user").
    private static boolean isBotBlocked(TelegramApiException e) {
        if (e instanceof TelegramApiRequestException request) {
            Integer code = request.getErrorCode();
            if (code != null && code == 403) {
                return true;
            }
        }
        String message = e.getMessage();
        return message != null && (message.contains("Forbidden") || message.contains("blocked"));
    }

    private Broadcast draft(VerifiedAdmin admin, String segment, String language, BroadcastRequest request,
                            String token, int total) {
        return new Broadcast(admin.id(), System.currentTimeMillis(), segment, language, request.text().trim(),
                trimToNull(request.photoUrl()), buttonText(request), buttonUrl(request), DRAFT, total, token);
    }

    private static BroadcastSummary toSummary(Broadcast b) {
        return new BroadcastSummary(b.getId(), b.getAdminId(), b.getCreatedAt(), b.getSegment(), b.getLanguage(),
                b.getStatus(), b.getSent(), b.getFailed(), b.getTotal());
    }

    private static String requireLanguage(BroadcastRequest request) {
        if (request.language() == null || request.language().isBlank()) {
            throw new IllegalArgumentException("language is required for the by-language segment");
        }
        return request.language().trim();
    }

    private static void validateContent(BroadcastRequest request) {
        if (request.text() == null || request.text().isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        boolean hasPhoto = request.photoUrl() != null && !request.photoUrl().isBlank();
        int max = hasPhoto ? MAX_CAPTION : MAX_TEXT;
        if (request.text().trim().length() > max) {
            throw new IllegalArgumentException("text exceeds " + max + " characters");
        }
        if (hasPhoto && !isHttpUrl(request.photoUrl().trim())) {
            throw new IllegalArgumentException("photoUrl must be an http(s) URL");
        }
        Button button = request.button();
        if (button != null) {
            if (button.text() == null || button.text().isBlank()) {
                throw new IllegalArgumentException("button text is required");
            }
            if (button.url() == null || !isHttpUrl(button.url().trim())) {
                throw new IllegalArgumentException("button url must be an http(s) URL");
            }
        }
    }

    private static String buttonText(BroadcastRequest request) {
        return request.button() == null ? null : request.button().text().trim();
    }

    private static String buttonUrl(BroadcastRequest request) {
        return request.button() == null ? null : request.button().url().trim();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean isHttpUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }
}
