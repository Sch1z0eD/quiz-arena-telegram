package com.quizarena.service;

import com.quizarena.bot.GameMessenger;
import com.quizarena.i18n.Localizer;
import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.GameStore;
import com.quizarena.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameServiceTest {

    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final GameSettings settings = mock(GameSettings.class);
    private final GameService service = new GameService(
            mock(GameStore.class), mock(GameMessenger.class), questions, mock(AnswerRepository.class),
            mock(TaskScheduler.class), settings, mock(Localizer.class), mock(LocaleService.class),
            mock(EloService.class), mock(AvatarService.class), mock(OptionShuffler.class));

    @BeforeEach
    void defaults() {
        when(settings.questionsPerGame()).thenReturn(5);
    }

    @Test
    void questionsPerGameSettingDrivesAvailability() {
        when(settings.questionsPerGame()).thenReturn(7);
        when(questions.categoriesWithMinQuestions("ru", 7)).thenReturn(List.of("science"));
        assertEquals(List.of("science"), service.availableCategories("ru"), "the configured count flows into the pool query");
    }

    @Test
    void hasEnoughQuestionsScopesToLanguage() {
        when(questions.countFiltered("science", "easy", "ru")).thenReturn(7L);
        assertTrue(service.hasEnoughQuestions("science", "easy", "ru"));
        // a different language is a different pool - unstubbed call returns 0
        assertFalse(service.hasEnoughQuestions("science", "easy", "en"));
    }

    @Test
    void availableCategoriesReadsDbSlugsSortedIncludingNewOnes() {
        when(questions.categoriesWithMinQuestions("ru", 5))
                .thenReturn(List.of("science", "russian-films-2026", "history"));
        assertEquals(List.of("history", "russian-films-2026", "science"), service.availableCategories("ru"));
    }
}
