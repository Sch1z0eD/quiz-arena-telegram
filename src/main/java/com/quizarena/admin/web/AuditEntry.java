package com.quizarena.admin.web;

import java.time.Instant;

public record AuditEntry(long id, Instant ts, long adminId, String action, String target, String details) {
}
