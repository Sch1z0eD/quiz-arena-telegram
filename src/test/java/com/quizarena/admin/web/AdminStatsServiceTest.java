package com.quizarena.admin.web;

import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.CategoryRepository;
import com.quizarena.repository.DuelRepository;
import com.quizarena.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminStatsServiceTest {

    private static final Instant T0 = Instant.parse("2026-06-21T10:00:00Z");

    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final AnswerRepository answers = mock(AnswerRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final DuelRepository duels = mock(DuelRepository.class);
    private final Clock clock = mock(Clock.class);
    private final AdminStatsService service = new AdminStatsService(questions, answers, categories, duels, clock);

    @BeforeEach
    void stubAggregates() {
        // Build projection mocks first: calling when() inside another when(...) argument breaks stubbing.
        QuestionRepository.NamedCount categoryRow = namedCount("science", 12);
        QuestionRepository.NamedCount difficultyRow = namedCount("easy", 8);
        QuestionRepository.NamedCount languageRow = namedCount("ru", 11);
        AnswerRepository.DayCount dayRow = dayCount("2026-06-21", 9);
        AnswerRepository.CategoryAnswerCount topRow = categoryAnswerCount("science", 30);

        when(answers.countDistinctUsers()).thenReturn(50L);
        when(answers.countDistinctUsersSince(anyLong())).thenReturn(10L);
        when(answers.countSoloGames()).thenReturn(7L);
        when(answers.countGroupGames()).thenReturn(3L);
        when(duels.count()).thenReturn(4L);
        when(questions.countByActive(true)).thenReturn(20L);
        when(questions.countByActive(false)).thenReturn(5L);
        when(questions.activeCountByCategory()).thenReturn(List.of(categoryRow));
        when(questions.activeCountByDifficulty()).thenReturn(List.of(difficultyRow));
        when(questions.activeCountByLanguage()).thenReturn(List.of(languageRow));
        when(categories.countByActive(true)).thenReturn(8L);
        when(categories.countByActive(false)).thenReturn(2L);
        when(answers.answersPerDay(anyLong())).thenReturn(List.of(dayRow));
        when(answers.topCategoriesByAnswers(anyInt())).thenReturn(List.of(topRow));
        when(answers.count()).thenReturn(200L);
        when(answers.countByCorrectTrue()).thenReturn(150L);
    }

    @Test
    void assemblesOverviewAndAccuracy() {
        when(clock.instant()).thenReturn(T0);

        OverviewResponse overview = service.overview();

        assertEquals(50, overview.players().total());
        assertEquals(10, overview.players().active7d());
        assertEquals(10, overview.players().active30d());
        assertEquals(7, overview.games().solo());
        assertEquals(3, overview.games().group());
        assertEquals(4, overview.games().duel());
        assertEquals(20, overview.questions().active());
        assertEquals(5, overview.questions().inactive());
        assertEquals("science", overview.questions().byCategory().get(0).name());
        assertEquals(8, overview.categories().active());
        assertEquals(2, overview.categories().hidden());
        assertEquals("2026-06-21", overview.answersPerDay().get(0).day());
        assertEquals(30, overview.topCategories().get(0).count());
        assertEquals(75, overview.accuracyPercent());
        assertEquals(200, overview.totalAnswers());
    }

    @Test
    void cachesWithinTtlAndRecomputesAfterExpiry() {
        when(clock.instant()).thenReturn(T0, T0.plusSeconds(30), T0.plusSeconds(61));

        service.overview(); // computes
        service.overview(); // within 60s TTL -> cached
        service.overview(); // past TTL -> recomputes

        verify(answers, times(2)).count();
    }

    private static QuestionRepository.NamedCount namedCount(String name, long count) {
        QuestionRepository.NamedCount row = mock(QuestionRepository.NamedCount.class);
        when(row.getName()).thenReturn(name);
        when(row.getCount()).thenReturn(count);
        return row;
    }

    private static AnswerRepository.DayCount dayCount(String day, long count) {
        AnswerRepository.DayCount row = mock(AnswerRepository.DayCount.class);
        when(row.getDay()).thenReturn(day);
        when(row.getCount()).thenReturn(count);
        return row;
    }

    private static AnswerRepository.CategoryAnswerCount categoryAnswerCount(String category, long count) {
        AnswerRepository.CategoryAnswerCount row = mock(AnswerRepository.CategoryAnswerCount.class);
        when(row.getCategory()).thenReturn(category);
        when(row.getCount()).thenReturn(count);
        return row;
    }
}
