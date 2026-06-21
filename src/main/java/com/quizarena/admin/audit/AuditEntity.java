package com.quizarena.admin.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant ts;

    @Column(name = "admin_id", nullable = false)
    private long adminId;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(length = 128)
    private String target;

    @Column
    private String details;

    protected AuditEntity() {
    }

    public AuditEntity(Instant ts, long adminId, String action, String target, String details) {
        this.ts = ts;
        this.adminId = adminId;
        this.action = action;
        this.target = target;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public Instant getTs() {
        return ts;
    }

    public long getAdminId() {
        return adminId;
    }

    public String getAction() {
        return action;
    }

    public String getTarget() {
        return target;
    }

    public String getDetails() {
        return details;
    }
}
