package com.quizarena.integration;

import com.quizarena.domain.AnswerRecord;
import com.quizarena.domain.CategoryEntity;
import com.quizarena.domain.CategoryTranslation;
import com.quizarena.domain.DuelRecord;
import com.quizarena.domain.Question;
import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.CategoryRepository;
import com.quizarena.repository.DuelRepository;
import com.quizarena.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatsRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private AnswerRepository answers;
    @Autowired
    private DuelRepository duels;
    @Autowired
    private QuestionRepository questions;
    @Autowired
    private CategoryRepository categories;

    @Test
    void distinctUsersCountsUniqueAnswerers() {
        long before = answers.countDistinctUsers();
        answers.save(ans(810001L, 1L, 990001L, 1L, true, 1000L, "GAME"));
        answers.save(ans(810001L, 1L, 990001L, 1L, true, 1000L, "GAME")); // same user twice -> still one distinct
        answers.save(ans(810002L, 1L, 990002L, 1L, true, 1000L, "GAME"));
        assertEquals(before + 2, answers.countDistinctUsers());
    }

    @Test
    void activePlayersScopeToSinceCutoff() {
        long future = Instant.parse("2031-01-01T00:00:00Z").toEpochMilli();
        long since = future - 1000L;
        long before = answers.countDistinctUsersSince(since);
        answers.save(ans(811001L, 1L, 991001L, 1L, true, future, "GAME"));
        answers.save(ans(811002L, 1L, 991002L, 1L, true, future, "GAME"));
        assertEquals(before + 2, answers.countDistinctUsersSince(since));
    }

    @Test
    void gameCountsSplitByModeAndChatSign() {
        long beforeSolo = answers.countSoloGames();
        long beforeGroup = answers.countGroupGames();
        answers.save(ans(820001L, 555L, 992001L, 1L, true, 1000L, "GAME"));   // solo (chat > 0)
        answers.save(ans(820002L, -555L, 992002L, 1L, true, 1000L, "GAME"));  // group (chat < 0)
        answers.save(ans(820003L, 600L, 992003L, 1L, true, 1000L, "DUEL"));   // duel mode -> neither

        assertEquals(beforeSolo + 1, answers.countSoloGames());
        assertEquals(beforeGroup + 1, answers.countGroupGames());
    }

    @Test
    void duelCountReflectsDuelRows() {
        long before = duels.count();
        duels.save(new DuelRecord(820500L, 1L, 2L, 3, 1, 1L, "science", "easy", 1000L));
        assertEquals(before + 1, duels.count());
    }

    @Test
    void accuracyCountsTrackCorrectAndTotal() {
        long beforeTotal = answers.count();
        long beforeCorrect = answers.countByCorrectTrue();
        answers.save(ans(830001L, 1L, 993001L, 1L, true, 1000L, "GAME"));
        answers.save(ans(830001L, 1L, 993001L, 1L, true, 1000L, "GAME"));
        answers.save(ans(830001L, 1L, 993001L, 1L, false, 1000L, "GAME"));
        assertEquals(beforeTotal + 3, answers.count());
        assertEquals(beforeCorrect + 2, answers.countByCorrectTrue());
    }

    @Test
    void answersPerDayBucketsByUtcDay() {
        long lateUtc = Instant.parse("2020-06-15T23:30:00Z").toEpochMilli();
        answers.save(ans(840001L, 1L, 994001L, 1L, true, lateUtc, "GAME"));
        answers.save(ans(840001L, 1L, 994001L, 1L, true, lateUtc, "GAME"));

        AnswerRepository.DayCount day = answers.answersPerDay(0L).stream()
                .filter(row -> "2020-06-15".equals(row.getDay())).findFirst().orElseThrow();
        assertEquals(2, day.getCount(), "23:30Z must bucket into the same UTC day, no other test uses 2020");
    }

    @Test
    void topCategoriesJoinsQuestionsAndHonoursLimit() {
        long q1 = questions.save(q("stt 1", "stats-top", "stt", "h-stt-1", true)).getId();
        long q2 = questions.save(q("stt 2", "stats-top", "stt", "h-stt-2", true)).getId();
        for (int i = 0; i < 5; i++) {
            answers.save(ans(850001L, 1L, 995001L, i % 2 == 0 ? q1 : q2, true, 1000L, "GAME"));
        }

        AnswerRepository.CategoryAnswerCount mine = answers.topCategoriesByAnswers(1000).stream()
                .filter(row -> "stats-top".equals(row.getCategory())).findFirst().orElseThrow();
        assertEquals(5, mine.getCount());
        assertEquals(1, answers.topCategoriesByAnswers(1).size(), "limit must cap the result size");
    }

    @Test
    void questionAndCategoryActiveBreakdowns() {
        long beforeActive = questions.countByActive(true);
        long beforeInactive = questions.countByActive(false);
        questions.save(q("brk 1", "stats-brk", "stb", "h-stb-1", true));
        questions.save(q("brk 2", "stats-brk", "stb", "h-stb-2", true));
        questions.save(q("brk 3", "stats-brk", "stb", "h-stb-3", true));
        questions.save(q("brk off", "stats-brk", "stb", "h-stb-4", false));
        assertEquals(beforeActive + 3, questions.countByActive(true));
        assertEquals(beforeInactive + 1, questions.countByActive(false));

        QuestionRepository.NamedCount byCategory = questions.activeCountByCategory().stream()
                .filter(row -> "stats-brk".equals(row.getName())).findFirst().orElseThrow();
        assertEquals(3, byCategory.getCount());

        long beforeCatsActive = categories.countByActive(true);
        long beforeCatsHidden = categories.countByActive(false);
        categories.save(category("stats-brk-on", true));
        categories.save(category("stats-brk-off", false));
        assertEquals(beforeCatsActive + 1, categories.countByActive(true));
        assertEquals(beforeCatsHidden + 1, categories.countByActive(false));
    }

    private static AnswerRecord ans(long game, long chat, long user, long questionId, boolean correct, long ts, String mode) {
        return new AnswerRecord(game, chat, user, questionId, correct, 0, ts, mode);
    }

    private static Question q(String text, String category, String language, String hash, boolean active) {
        Question question = new Question(text, "A", "B", "C", "D", 0, category, "easy", language, hash);
        question.setActive(active);
        return question;
    }

    private static CategoryEntity category(String slug, boolean active) {
        CategoryEntity category = new CategoryEntity(slug);
        category.setActive(active);
        category.addTranslation(new CategoryTranslation(category, "en", slug));
        return category;
    }
}
