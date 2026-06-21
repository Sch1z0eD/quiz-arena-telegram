package com.quizarena.admin.web;

import com.quizarena.admin.web.OverviewResponse.CategoryStats;
import com.quizarena.admin.web.OverviewResponse.DailyCount;
import com.quizarena.admin.web.OverviewResponse.Games;
import com.quizarena.admin.web.OverviewResponse.NamedCount;
import com.quizarena.admin.web.OverviewResponse.Players;
import com.quizarena.admin.web.OverviewResponse.QuestionBreakdown;
import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.CategoryRepository;
import com.quizarena.repository.DuelRepository;
import com.quizarena.repository.QuestionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class AdminStatsService {

    private static final Duration OVERVIEW_TTL = Duration.ofSeconds(60);
    private static final int ACTIVITY_DAYS = 30;
    private static final int TOP_CATEGORIES = 10;

    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final CategoryRepository categories;
    private final DuelRepository duels;
    private final Clock clock;

    private volatile Cached cached;

    public AdminStatsService(QuestionRepository questions, AnswerRepository answers,
                             CategoryRepository categories, DuelRepository duels, Clock clock) {
        this.questions = questions;
        this.answers = answers;
        this.categories = categories;
        this.duels = duels;
        this.clock = clock;
    }

    public StatsResponse snapshot() {
        return new StatsResponse(questions.count(), answers.count());
    }

    public OverviewResponse overview() {
        Instant now = clock.instant();
        Cached current = cached;
        if (current != null && now.isBefore(current.expiresAt())) {
            return current.value();
        }
        OverviewResponse value = compute(now);
        cached = new Cached(now.plus(OVERVIEW_TTL), value);
        return value;
    }

    private OverviewResponse compute(Instant now) {
        long since7d = now.minus(7, ChronoUnit.DAYS).toEpochMilli();
        long since30d = now.minus(ACTIVITY_DAYS, ChronoUnit.DAYS).toEpochMilli();

        Players players = new Players(answers.countDistinctUsers(),
                answers.countDistinctUsersSince(since7d), answers.countDistinctUsersSince(since30d));
        Games games = new Games(answers.countSoloGames(), answers.countGroupGames(), duels.count());
        QuestionBreakdown questionBreakdown = new QuestionBreakdown(
                questions.countByActive(true), questions.countByActive(false),
                buckets(questions.activeCountByCategory()), buckets(questions.activeCountByDifficulty()),
                buckets(questions.activeCountByLanguage()));
        CategoryStats categoryStats = new CategoryStats(categories.countByActive(true), categories.countByActive(false));
        List<DailyCount> perDay = answers.answersPerDay(since30d).stream()
                .map(row -> new DailyCount(row.getDay(), row.getCount())).toList();
        List<NamedCount> top = answers.topCategoriesByAnswers(TOP_CATEGORIES).stream()
                .map(row -> new NamedCount(row.getCategory(), row.getCount())).toList();

        long totalAnswers = answers.count();
        long correct = answers.countByCorrectTrue();
        int accuracy = totalAnswers == 0 ? 0 : (int) Math.round(correct * 100.0 / totalAnswers);

        return new OverviewResponse(players, games, questionBreakdown, categoryStats, perDay, top, accuracy, totalAnswers);
    }

    private static List<NamedCount> buckets(List<QuestionRepository.NamedCount> rows) {
        return rows.stream().map(row -> new NamedCount(row.getName(), row.getCount())).toList();
    }

    private record Cached(Instant expiresAt, OverviewResponse value) {
    }
}
