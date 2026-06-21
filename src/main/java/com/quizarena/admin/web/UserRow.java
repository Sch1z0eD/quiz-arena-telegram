package com.quizarena.admin.web;

public record UserRow(long id, String name, String username, String language, long games,
                      Integer accuracyPercent, int elo, long firstSeen, long lastSeen,
                      boolean banned, boolean blocked) {
}
