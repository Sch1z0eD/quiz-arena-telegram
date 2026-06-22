package com.quizarena.admin.web;

import com.quizarena.admin.auth.VerifiedAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/broadcasts")
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class AdminBroadcastController {

    private static final int MAX_PAGE_SIZE = 100;

    private final BroadcastService service;

    public AdminBroadcastController(BroadcastService service) {
        this.service = service;
    }

    @PostMapping("/dry-run")
    public DryRunResponse dryRun(@AuthenticationPrincipal VerifiedAdmin admin, @RequestBody BroadcastRequest request) {
        return service.dryRun(admin, request);
    }

    @PostMapping("/test")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BroadcastSummary test(@AuthenticationPrincipal VerifiedAdmin admin, @RequestBody BroadcastRequest request) {
        return service.test(admin, request);
    }

    @PostMapping("/{id}/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void start(@AuthenticationPrincipal VerifiedAdmin admin, @PathVariable long id,
                      @RequestBody StartRequest request) {
        service.start(admin, id, request.token());
    }

    @PostMapping("/{id}/abort")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void abort(@AuthenticationPrincipal VerifiedAdmin admin, @PathVariable long id) {
        service.abort(admin, id);
    }

    @GetMapping
    public PageResponse<BroadcastSummary> history(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return service.history(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    @GetMapping("/{id}")
    public BroadcastSummary status(@PathVariable long id) {
        return service.status(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
