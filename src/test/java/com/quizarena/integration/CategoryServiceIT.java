package com.quizarena.integration;

import com.quizarena.domain.Category;
import com.quizarena.domain.CategoryEntity;
import com.quizarena.domain.CategoryTranslation;
import com.quizarena.repository.CategoryRepository;
import com.quizarena.service.CategoryService;
import com.quizarena.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryServiceIT extends AbstractIntegrationTest {

    private static final Locale RU = Locale.of("ru");
    private static final Locale EN = Locale.of("en");

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private GameService gameService;
    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void seedsCurrentCategoryNames() {
        assertEquals("Наука", categoryService.name("science", RU));
        assertEquals("Science", categoryService.name("science", EN));
        assertEquals("Видеоигры", categoryService.name("games", RU));
        assertEquals("Video games", categoryService.name("games", EN));
        assertEquals("Общие знания", categoryService.name("general", RU));
        assertEquals("Mythology", categoryService.name("mythology", EN));
    }

    @Test
    void unknownSlugFallsBackToSlug() {
        assertEquals("nosuchcategory", categoryService.name("nosuchcategory", RU));
    }

    @Test
    void pickerCategoriesResolveToDbNames() {
        List<Category> categories = gameService.availableCategories("ru");
        assertFalse(categories.isEmpty(), "the ru picker should not be empty for the seeded data");
        for (Category category : categories) {
            assertNotEquals(category.slug(), categoryService.name(category.slug(), RU),
                    "every picker category must resolve to a real DB name, not the slug fallback");
        }
    }

    @Test
    void persistsCategoryWithTranslationsAndCascadesOnDelete() {
        CategoryEntity category = new CategoryEntity("ittestcat");
        category.addTranslation(new CategoryTranslation(category, "ru", "ИТ-тест"));
        category.addTranslation(new CategoryTranslation(category, "en", "IT test"));
        categoryRepository.save(category);

        CategoryEntity loaded = categoryRepository.findBySlug("ittestcat").orElseThrow();
        assertEquals(2, loaded.getTranslations().size());

        categoryRepository.delete(loaded);
        assertTrue(categoryRepository.findBySlug("ittestcat").isEmpty());
    }
}
