package com.quizarena.service;

import com.quizarena.bot.GameMessenger;
import com.quizarena.config.GameProperties;
import com.quizarena.domain.Category;
import com.quizarena.i18n.Localizer;
import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.GameStore;
import com.quizarena.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameServiceTest {

    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final GameProperties properties = new GameProperties(20, 15, 100, 5, Duration.ofHours(1));
    private final GameService service = new GameService(
            mock(GameStore.class), mock(GameMessenger.class), questions, mock(AnswerRepository.class),
            mock(TaskScheduler.class), properties, mock(Localizer.class), mock(LocaleService.class),
            mock(EloService.class));

    @Test
    void hasEnoughQuestionsScopesToLanguage() {
        when(questions.countFiltered("science", "easy", "ru")).thenReturn(7L);
        assertTrue(service.hasEnoughQuestions("science", "easy", "ru"));
        // a different language is a different pool - unstubbed call returns 0
        assertFalse(service.hasEnoughQuestions("science", "easy", "en"));
    }

    @Test
    void availableCategoriesScopesToLanguage() {
        when(questions.categoriesWithMinQuestions("ru", 5)).thenReturn(List.of("science", "history"));
        assertEquals(List.of(Category.SCIENCE, Category.HISTORY), service.availableCategories("ru"));
    }
}
