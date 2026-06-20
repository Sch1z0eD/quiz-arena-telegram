package com.quizarena.domain;

public record DuelInvite(long inviterUserId, long inviterChatId, String locale, String category,
                         String difficulty, String inviterName) {}
