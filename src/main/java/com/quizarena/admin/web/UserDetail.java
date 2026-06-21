package com.quizarena.admin.web;

import java.util.List;

public record UserDetail(UserRow summary, List<CategoryStat> categories, List<RecentGame> recentGames, DuelSummary duel) {

    public record CategoryStat(String category, long answered, Integer accuracyPercent) {
    }

    public record RecentGame(long gameId, String mode, long finishedAt, long correct, long total) {
    }

    public record DuelSummary(long played, long wins, long draws, long losses) {
    }
}
