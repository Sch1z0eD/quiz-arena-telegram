package com.quizarena.admin.web;

import com.quizarena.admin.audit.AuditService;
import com.quizarena.admin.auth.VerifiedAdmin;
import com.quizarena.domain.CategoryEntity;
import com.quizarena.domain.CategoryTranslation;
import com.quizarena.domain.Language;
import com.quizarena.repository.CategoryRepository;
import com.quizarena.repository.QuestionRepository;
import com.quizarena.service.CategoryService;
import com.quizarena.service.LanguageRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminCategoryServiceTest {

    private static final VerifiedAdmin ADMIN = new VerifiedAdmin(1, "Admin");

    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final AuditService audit = mock(AuditService.class);
    private final LanguageRegistry languageRegistry = mock(LanguageRegistry.class);
    private final AdminCategoryService service =
            new AdminCategoryService(categories, questions, categoryService, audit, languageRegistry);

    @BeforeEach
    void enableSeededLanguages() {
        lenient().when(languageRegistry.enabled())
                .thenReturn(List.of(new Language("en", "English"), new Language("ru", "Русский")));
    }

    @Test
    void createInactiveByDefaultPersistsAuditsAndRefreshes() {
        when(categories.existsBySlug("science")).thenReturn(false);

        service.create(ADMIN, Map.of("ru", "Наука", "en", "Science"), false);

        ArgumentCaptor<CategoryEntity> saved = ArgumentCaptor.forClass(CategoryEntity.class);
        verify(categories).save(saved.capture());
        assertEquals("science", saved.getValue().getSlug());
        assertFalse(saved.getValue().isActive(), "a category created with active=false must be hidden");
        verify(audit).record(eq(ADMIN), eq("category.created"), eq("science"), anyString());
        verify(categoryService).refresh();
    }

    @Test
    void createCanStartActive() {
        when(categories.existsBySlug("science")).thenReturn(false);

        service.create(ADMIN, Map.of("ru", "Наука", "en", "Science"), true);

        ArgumentCaptor<CategoryEntity> saved = ArgumentCaptor.forClass(CategoryEntity.class);
        verify(categories).save(saved.capture());
        assertTrue(saved.getValue().isActive());
    }

    @Test
    void createMakesSlugUniqueAgainstExisting() {
        when(categories.existsBySlug("science")).thenReturn(true);
        when(categories.existsBySlug("science-2")).thenReturn(false);

        service.create(ADMIN, Map.of("ru", "Наука вторая", "en", "Science"), false);

        ArgumentCaptor<CategoryEntity> saved = ArgumentCaptor.forClass(CategoryEntity.class);
        verify(categories).save(saved.capture());
        assertEquals("science-2", saved.getValue().getSlug());
    }

    @Test
    void createRejectsMissingName() {
        assertThrows(IllegalArgumentException.class, () -> service.create(ADMIN, Map.of("ru", "Наука"), false));
        verify(categories, never()).save(any());
    }

    @Test
    void enableTogglesActiveAuditsAndRefreshes() {
        CategoryEntity category = new CategoryEntity("science");
        category.setActive(false);
        when(categories.findBySlug("science")).thenReturn(Optional.of(category));

        service.setActive(ADMIN, "science", true);

        assertTrue(category.isActive());
        verify(audit).record(eq(ADMIN), eq("category.enabled"), eq("science"), isNull());
        verify(categoryService).refresh();
    }

    @Test
    void disableTogglesActiveAuditsAndRefreshes() {
        CategoryEntity category = new CategoryEntity("science");
        when(categories.findBySlug("science")).thenReturn(Optional.of(category));

        service.setActive(ADMIN, "science", false);

        assertFalse(category.isActive());
        verify(audit).record(eq(ADMIN), eq("category.disabled"), eq("science"), isNull());
        verify(categoryService).refresh();
    }

    @Test
    void updateRenamesAuditsAndRefreshes() {
        CategoryEntity category = new CategoryEntity("science");
        category.addTranslation(new CategoryTranslation(category, "ru", "Наука"));
        category.addTranslation(new CategoryTranslation(category, "en", "Science"));
        when(categories.findBySlug("science")).thenReturn(Optional.of(category));

        service.update(ADMIN, "science", Map.of("ru", "Естествознание", "en", "Natural science"));

        Map<String, String> names = category.getTranslations().stream()
                .collect(Collectors.toMap(CategoryTranslation::getLanguage, CategoryTranslation::getName));
        assertEquals("Естествознание", names.get("ru"));
        assertEquals("Natural science", names.get("en"));
        verify(audit).record(eq(ADMIN), eq("category.updated"), eq("science"), anyString());
        verify(categoryService).refresh();
    }

    @Test
    void deleteForbiddenWhenQuestionsExist() {
        CategoryEntity category = new CategoryEntity("science");
        when(categories.findBySlug("science")).thenReturn(Optional.of(category));
        when(questions.countByCategory("science")).thenReturn(5L);

        CategoryInUseException exception =
                assertThrows(CategoryInUseException.class, () -> service.delete(ADMIN, "science"));
        assertEquals(5, exception.questionCount());
        verify(categories, never()).delete(any());
        verify(categoryService, never()).refresh();
    }

    @Test
    void deleteAllowedWhenEmpty() {
        CategoryEntity category = new CategoryEntity("empty");
        when(categories.findBySlug("empty")).thenReturn(Optional.of(category));
        when(questions.countByCategory("empty")).thenReturn(0L);

        service.delete(ADMIN, "empty");

        verify(categories).delete(category);
        verify(audit).record(eq(ADMIN), eq("category.deleted"), eq("empty"), isNull());
        verify(categoryService).refresh();
    }
}
