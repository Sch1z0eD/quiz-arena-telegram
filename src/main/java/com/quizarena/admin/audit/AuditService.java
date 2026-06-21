package com.quizarena.admin.audit;

import com.quizarena.admin.auth.VerifiedAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Clock;

/**
 * Append-only audit trail for admin mutations. Called inside the mutation's transaction so a rolled-back
 * change leaves no audit row. Reads are never audited.
 */
@Service
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class AuditService {

    private final AuditRepository repository;
    private final Clock clock;

    public AuditService(AuditRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void record(VerifiedAdmin admin, String action, String target, String details) {
        repository.save(new AuditEntity(clock.instant(), admin.id(), action, target, details));
    }
}
