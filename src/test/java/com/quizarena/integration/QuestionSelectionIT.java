package com.quizarena.integration;

import com.quizarena.domain.Question;
import com.quizarena.repository.QuestionRepository;
import com.quizarena.service.QuestionHash;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionSelectionIT extends AbstractIntegrationTest {

    @Autowired
    private QuestionRepository questions;

    @Test
    void selectionQueriesIgnoreInactiveQuestions() {
        questions.save(question("act-a one", "science", "easy", "act-a", "h-act-a-1", true));
        questions.save(question("act-a two", "science", "easy", "act-a", "h-act-a-2", true));
        questions.save(question("act-a three", "science", "easy", "act-a", "h-act-a-3", true));
        questions.save(question("act-a hidden", "science", "easy", "act-a", "h-act-a-4", false));

        long count = questions.countFiltered("science", "easy", "act-a");
        assertEquals(3, count, "countFiltered must skip the inactive question");

        List<Question> drawn = questions.findRandomFiltered("science", "easy", "act-a", 10);
        assertEquals(3, drawn.size(), "findRandomFiltered must not draw the inactive question");
        assertTrue(drawn.stream().allMatch(Question::isActive));
    }

    @Test
    void categoryPickerCountsOnlyActive() {
        questions.save(question("act-b one", "science", "easy", "act-b", "h-act-b-1", true));
        questions.save(question("act-b two", "science", "easy", "act-b", "h-act-b-2", false));
        questions.save(question("act-b three", "science", "easy", "act-b", "h-act-b-3", false));

        assertTrue(questions.categoriesWithMinQuestions("act-b", 1).contains("science"));
        assertFalse(questions.categoriesWithMinQuestions("act-b", 2).contains("science"),
                "two of three are inactive, so the category must not meet a minimum of two");
    }

    @Test
    void difficultyPickerCountsOnlyActive() {
        questions.save(question("act-c one", "science", "easy", "act-c", "h-act-c-1", true));
        questions.save(question("act-c two", "science", "easy", "act-c", "h-act-c-2", true));
        questions.save(question("act-c hard", "science", "hard", "act-c", "h-act-c-3", false));

        List<String> available = questions.difficultiesWithMinQuestions("science", "act-c", 1);
        assertTrue(available.contains("easy"));
        assertFalse(available.contains("hard"), "the only hard question is inactive");
    }

    @Test
    void hashHelperReproducesStoredHashOfSeededQuestion() {
        Question seeded = questions.search("Столица Австралии", "", "", "ru", PageRequest.of(0, 1))
                .getContent().get(0);
        assertEquals(seeded.getQuestionHash(), QuestionHash.of(seeded.getText()),
                "the shared hash helper must reproduce the hash stored by the importer and the V2 backfill");
    }

    private static Question question(String text, String category, String difficulty, String language,
                                     String hash, boolean active) {
        Question question = new Question(text, "A", "B", "C", "D", 0, category, difficulty, language, hash);
        question.setActive(active);
        return question;
    }
}
