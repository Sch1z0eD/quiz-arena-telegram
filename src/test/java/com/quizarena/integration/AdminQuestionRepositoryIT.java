package com.quizarena.integration;

import com.quizarena.domain.AnswerRecord;
import com.quizarena.domain.Question;
import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminQuestionRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private QuestionRepository questions;
    @Autowired
    private AnswerRepository answers;

    @Test
    void searchFiltersAndPaginates() {
        Question volcano = questions.save(question("Volcano facts", "science", "easy", "qa1", "h-qa1-1"));
        questions.save(question("Star physics", "science", "hard", "qa1", "h-qa1-2"));
        questions.save(question("Ancient Rome", "history", "easy", "qa1", "h-qa1-3"));

        Page<Question> firstPage = questions.search("", "", "", "qa1", PageRequest.of(0, 2, Sort.by("id")));
        assertEquals(3, firstPage.getTotalElements());
        assertEquals(2, firstPage.getContent().size());
        assertEquals(2, firstPage.getTotalPages());

        assertEquals(2, questions.search("", "science", "", "qa1", PageRequest.of(0, 10, Sort.by("id"))).getTotalElements());
        assertEquals(1, questions.search("", "science", "hard", "qa1", PageRequest.of(0, 10, Sort.by("id"))).getTotalElements());

        Page<Question> byText = questions.search("volcano", "", "", "qa1", PageRequest.of(0, 10, Sort.by("id")));
        assertEquals(1, byText.getTotalElements());
        assertEquals(volcano.getId(), byText.getContent().get(0).getId());
    }

    @Test
    void categoryCountsGroupByLanguage() {
        questions.save(question("geo one", "geography", "easy", "qa2", "h-qa2-1"));
        questions.save(question("geo two", "geography", "medium", "qa2", "h-qa2-2"));

        long geographyQa2 = questions.categoryCounts().stream()
                .filter(c -> "geography".equals(c.getCategory()) && "qa2".equals(c.getLanguage()))
                .mapToLong(QuestionRepository.CategoryLanguageCount::getCount)
                .sum();
        assertEquals(2, geographyQa2);
    }

    @Test
    void correctOptionDistributionGroupsByCategory() {
        questions.save(new Question("dist a1", "A", "B", "C", "D", 0, "distcat", "easy", "dst", "h-dist-1"));
        questions.save(new Question("dist a2", "A", "B", "C", "D", 0, "distcat", "easy", "dst", "h-dist-2"));
        questions.save(new Question("dist b1", "A", "B", "C", "D", 1, "distcat", "easy", "dst", "h-dist-3"));
        questions.save(new Question("dist c1", "A", "B", "C", "D", 2, "distcat", "easy", "dst", "h-dist-4"));

        QuestionRepository.CorrectPositionRow row = questions.correctOptionDistribution().stream()
                .filter(r -> "distcat".equals(r.getCategory())).findFirst().orElseThrow();
        assertEquals(2, row.getA());
        assertEquals(1, row.getB());
        assertEquals(1, row.getC());
        assertEquals(0, row.getD());
        assertEquals(4, row.getTotal());
    }

    @Test
    void answerCountsPerQuestion() {
        long id = questions.save(question("counted", "music", "easy", "qa3", "h-qa3-1")).getId();
        answers.save(answer(id, true));
        answers.save(answer(id, true));
        answers.save(answer(id, false));

        assertEquals(3, answers.countByQuestionId(id));
        assertEquals(2, answers.countByQuestionIdAndCorrectTrue(id));
    }

    private static Question question(String text, String category, String difficulty, String language, String hash) {
        return new Question(text, "A", "B", "C", "D", 0, category, difficulty, language, hash);
    }

    private static AnswerRecord answer(long questionId, boolean correct) {
        return new AnswerRecord(1L, -1L, 1L, questionId, correct, 100, 0L, "SOLO");
    }
}
