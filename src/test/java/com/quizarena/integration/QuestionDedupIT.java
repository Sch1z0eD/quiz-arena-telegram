package com.quizarena.integration;

import com.quizarena.domain.Question;
import com.quizarena.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionDedupIT extends AbstractIntegrationTest {

    @Autowired
    private QuestionRepository questions;

    // Language "zz" keeps these rows out of the game queries, which filter by real languages.
    @Test
    void duplicateMd5HashIsRejectedByTheUniqueConstraint() {
        String hash = "it-dedup-" + System.nanoTime();
        Question first = questions.saveAndFlush(
                new Question("First?", "A", "B", "C", "D", 0, "general", "easy", "zz", hash));
        try {
            assertTrue(questions.existsByQuestionHash(hash), "importer's pre-check sees the existing hash");

            assertThrows(DataIntegrityViolationException.class, () -> questions.saveAndFlush(
                    new Question("Different text?", "A", "B", "C", "D", 1, "science", "hard", "zz", hash)),
                    "a second row with the same md5 must be rejected, so re-imports never duplicate");
        } finally {
            questions.deleteById(first.getId());
        }
    }
}
