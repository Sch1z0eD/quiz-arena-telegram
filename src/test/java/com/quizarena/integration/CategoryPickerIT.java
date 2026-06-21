package com.quizarena.integration;

import com.quizarena.config.GameProperties;
import com.quizarena.domain.CategoryEntity;
import com.quizarena.domain.CategoryTranslation;
import com.quizarena.domain.Question;
import com.quizarena.repository.CategoryRepository;
import com.quizarena.repository.QuestionRepository;
import com.quizarena.service.CategoryService;
import com.quizarena.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryPickerIT extends AbstractIntegrationTest {

    @Autowired
    private GameService gameService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private QuestionRepository questions;
    @Autowired
    private CategoryRepository categories;
    @Autowired
    private GameProperties properties;

    @Test
    void newDbCategoryBecomesPickableWithEnoughActiveQuestions() {
        int min = properties.questionsPerGame();
        saveCategory("russian-films-2026", "Русские фильмы 2026", "Russian films 2026", true);
        saveCategory("sparse-films", "Мало фильмов", "Sparse films", true);
        categoryService.refresh();

        for (int i = 0; i < min; i++) {
            questions.save(question("rf " + i, "russian-films-2026", "rfl", "h-rfl-" + i, true));
        }
        for (int i = 0; i < min - 1; i++) {
            questions.save(question("sf " + i, "sparse-films", "rfl", "h-sf-" + i, true));
        }

        List<String> picker = gameService.availableCategories("rfl");
        assertTrue(picker.contains("russian-films-2026"), "new category with enough active questions must be pickable");
        assertFalse(picker.contains("sparse-films"), "a category short of the minimum stays out of the picker");
        assertEquals("Русские фильмы 2026", categoryService.name("russian-films-2026", Locale.of("ru")));
        assertFalse(questions.findRandomFiltered("russian-films-2026", "", "rfl", min).isEmpty(),
                "game and duel selection must draw from the new category");
    }

    @Test
    void inactiveQuestionsDoNotMakeCategoryPickable() {
        int min = properties.questionsPerGame();
        saveCategory("hidden-films", "Скрытые вопросы", "Hidden questions", true);
        for (int i = 0; i < min; i++) {
            questions.save(question("hidden " + i, "hidden-films", "rfl2", "h-hf-" + i, false));
        }
        assertFalse(gameService.availableCategories("rfl2").contains("hidden-films"));
    }

    @Test
    void disablingCategoryRemovesItFromPickerDespiteEnoughQuestions() {
        int min = properties.questionsPerGame();
        saveCategory("ready-films", "Готовая категория", "Ready films", true);
        for (int i = 0; i < min; i++) {
            questions.save(question("ready " + i, "ready-films", "rfl3", "h-rf3-" + i, true));
        }
        assertTrue(gameService.availableCategories("rfl3").contains("ready-films"));

        CategoryEntity category = categories.findBySlug("ready-films").orElseThrow();
        category.setActive(false);
        categories.save(category);

        assertFalse(gameService.availableCategories("rfl3").contains("ready-films"),
                "a disabled category must leave the picker even with enough active questions");
    }

    private void saveCategory(String slug, String ru, String en, boolean active) {
        CategoryEntity category = new CategoryEntity(slug);
        category.setActive(active);
        category.addTranslation(new CategoryTranslation(category, "ru", ru));
        category.addTranslation(new CategoryTranslation(category, "en", en));
        categories.save(category);
    }

    private static Question question(String text, String category, String language, String hash, boolean active) {
        Question question = new Question(text, "A", "B", "C", "D", 0, category, "easy", language, hash);
        question.setActive(active);
        return question;
    }
}
