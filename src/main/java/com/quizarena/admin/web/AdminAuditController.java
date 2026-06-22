package com.quizarena.admin.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/audit")
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class AdminAuditController {

    private static final Set<String> SORTABLE = Set.of("ts", "action", "adminId", "id");

    private final AdminAuditService service;

    public AdminAuditController(AdminAuditService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AuditEntry> list(
            @RequestParam(required = false, defaultValue = "") String action,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false, defaultValue = "") String target,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "ts", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(action, adminId, target, from, to,
                Pageables.sanitize(pageable, SORTABLE, Sort.by(Sort.Order.desc("ts"))));
    }

    @GetMapping("/actions")
    public List<String> actions() {
        return service.actions();
    }

}
