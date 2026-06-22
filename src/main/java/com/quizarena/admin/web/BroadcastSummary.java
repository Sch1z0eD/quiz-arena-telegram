package com.quizarena.admin.web;

public record BroadcastSummary(long id, long adminId, long createdAt, String segment, String language,
                               String status, int sent, int failed, int total) {
}
