package com.quizarena.admin.web;

import com.quizarena.admin.auth.VerifiedAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class AdminUserController {

    private static final Set<String> SORTABLE = Set.of("id", "name", "games", "accuracy", "elo", "firstSeen", "lastSeen");

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<UserRow> users(
            @RequestParam(required = false, defaultValue = "") String q,
            @PageableDefault(size = 20, sort = "lastSeen", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(q, Pageables.sanitize(pageable, SORTABLE, Sort.by(Sort.Order.desc("lastSeen"))));
    }

    @GetMapping("/{id}")
    public UserDetail user(@PathVariable long id) {
        return service.detail(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}/banned")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setBanned(@AuthenticationPrincipal VerifiedAdmin admin, @PathVariable long id,
                          @RequestBody BannedRequest request) {
        service.setBanned(admin, id, request.banned());
    }

}
