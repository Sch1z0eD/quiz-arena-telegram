package com.quizarena.admin.web;

import java.util.List;

public record OverviewResponse(
        Players players,
        Games games,
        QuestionBreakdown questions,
        CategoryStats categories,
        List<DailyCount> answersPerDay,
        List<NamedCount> topCategories,
        int accuracyPercent,
        long totalAnswers) {

    public record Players(long total, long active7d, long active30d) {
    }

    public record Games(long solo, long group, long duel) {
    }

    public record QuestionBreakdown(long active, long inactive, List<NamedCount> byCategory,
                                    List<NamedCount> byDifficulty, List<NamedCount> byLanguage) {
    }

    public record CategoryStats(long active, long hidden) {
    }

    public record NamedCount(String name, long count) {
    }

    public record DailyCount(String day, long count) {
    }
}
