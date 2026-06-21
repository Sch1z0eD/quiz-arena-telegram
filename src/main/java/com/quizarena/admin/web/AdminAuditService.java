package com.quizarena.admin.web;

import com.quizarena.admin.audit.AuditEntity;
import com.quizarena.admin.audit.AuditRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class AdminAuditService {

    // Bounds used when a date filter is absent; Postgres needs concrete-typed params, not typeless nulls.
    // Public so the repository integration test can prove these exact values stay within timestamptz range.
    public static final Instant MIN_TS = Instant.EPOCH;
    public static final Instant MAX_TS = Instant.parse("9999-12-31T23:59:59Z");

    private final AuditRepository audit;

    public AdminAuditService(AuditRepository audit) {
        this.audit = audit;
    }

    public PageResponse<AuditEntry> list(String action, Long adminId, String target,
                                         Instant from, Instant to, Pageable pageable) {
        Page<AuditEntity> page = audit.search(nz(action), adminId == null ? 0L : adminId, nz(target),
                from == null ? MIN_TS : from, to == null ? MAX_TS : to, pageable);
        List<AuditEntry> content = page.getContent().stream()
                .map(e -> new AuditEntry(e.getId(), e.getTs(), e.getAdminId(), e.getAction(), e.getTarget(), e.getDetails()))
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public List<String> actions() {
        return audit.distinctActions();
    }

    private static String nz(String value) {
        return value == null ? "" : value.trim();
    }
}
