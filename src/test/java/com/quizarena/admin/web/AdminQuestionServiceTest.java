package com.quizarena.admin.web;

import com.quizarena.domain.Question;
import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminQuestionServiceTest {

    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final AnswerRepository answers = mock(AnswerRepository.class);
    private final AdminQuestionService service = new AdminQuestionService(questions, answers);

    @Test
    void detailComputesAccuracyFromAnswers() {
        when(questions.findById(7L)).thenReturn(Optional.of(question(7L)));
        when(answers.countByQuestionId(7L)).thenReturn(4L);
        when(answers.countByQuestionIdAndCorrectTrue(7L)).thenReturn(3L);

        QuestionDetail detail = service.detail(7L).orElseThrow();

        assertEquals(7L, detail.id());
        assertEquals(4, detail.stats().answered());
        assertEquals(3, detail.stats().correct());
        assertEquals(75, detail.stats().accuracyPercent());
        assertEquals(List.of("A", "B", "C", "D"), detail.options());
        assertEquals("science", detail.category());
        assertEquals("h7", detail.hash());
    }

    @Test
    void detailAccuracyIsZeroWithoutAnswers() {
        when(questions.findById(7L)).thenReturn(Optional.of(question(7L)));
        when(answers.countByQuestionId(7L)).thenReturn(0L);
        when(answers.countByQuestionIdAndCorrectTrue(7L)).thenReturn(0L);

        assertEquals(0, service.detail(7L).orElseThrow().stats().accuracyPercent());
    }

    @Test
    void detailEmptyWhenMissing() {
        when(questions.findById(9L)).thenReturn(Optional.empty());
        assertTrue(service.detail(9L).isEmpty());
    }

    @Test
    void listMapsPageMetadataAndContent() {
        when(questions.search("", "", "", "", PageRequest.of(1, 20)))
                .thenReturn(new PageImpl<>(List.of(question(7L)), PageRequest.of(1, 20), 45));

        PageResponse<QuestionSummary> response = service.list(null, null, null, null, PageRequest.of(1, 20));

        assertEquals(1, response.page());
        assertEquals(20, response.size());
        assertEquals(45, response.totalElements());
        assertEquals(3, response.totalPages());
        assertEquals(7L, response.content().get(0).id());
        assertEquals("science", response.content().get(0).category());
    }

    @Test
    void categoriesGroupByLanguageAndSumTotals() {
        when(questions.categoryCounts()).thenReturn(List.of(
                count("science", "en", 5), count("science", "ru", 3), count("history", "en", 2)));

        List<CategoryRow> rows = service.categories();

        assertEquals(2, rows.size());
        assertEquals("history", rows.get(0).slug());
        assertEquals("science", rows.get(1).slug());
        assertEquals(8, rows.get(1).total());
        assertEquals(5L, rows.get(1).byLanguage().get("en"));
        assertEquals(3L, rows.get(1).byLanguage().get("ru"));
    }

    private static Question question(long id) {
        Question q = new Question("Q text", "A", "B", "C", "D", 1, "science", "easy", "en", "h" + id);
        ReflectionTestUtils.setField(q, "id", id);
        return q;
    }

    private static QuestionRepository.CategoryLanguageCount count(String category, String language, long n) {
        return new QuestionRepository.CategoryLanguageCount() {
            @Override
            public String getCategory() {
                return category;
            }

            @Override
            public String getLanguage() {
                return language;
            }

            @Override
            public long getCount() {
                return n;
            }
        };
    }
}
