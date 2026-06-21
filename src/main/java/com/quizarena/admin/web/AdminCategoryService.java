package com.quizarena.admin.web;

import com.quizarena.admin.audit.AuditService;
import com.quizarena.admin.auth.VerifiedAdmin;
import com.quizarena.domain.CategoryEntity;
import com.quizarena.domain.CategoryTranslation;
import com.quizarena.repository.CategoryRepository;
import com.quizarena.repository.QuestionRepository;
import com.quizarena.service.CategoryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class AdminCategoryService {

    private static final List<String> LANGUAGES = List.of("ru", "en");
    private static final int MAX_NAME_LENGTH = 128;

    private final CategoryRepository categories;
    private final QuestionRepository questions;
    private final CategoryService categoryService;
    private final AuditService audit;

    public AdminCategoryService(CategoryRepository categories, QuestionRepository questions,
                                CategoryService categoryService, AuditService audit) {
        this.categories = categories;
        this.questions = questions;
        this.categoryService = categoryService;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CategoryRow> list() {
        Map<String, Map<String, Long>> countsBySlug = new LinkedHashMap<>();
        for (QuestionRepository.CategoryLanguageCount count : questions.categoryCounts()) {
            countsBySlug.computeIfAbsent(count.getCategory(), key -> new TreeMap<>())
                    .put(count.getLanguage(), count.getCount());
        }
        return categories.findAllWithTranslations().stream()
                .map(category -> toRow(category, countsBySlug.getOrDefault(category.getSlug(), Map.of())))
                .sorted(Comparator.comparing(CategoryRow::slug))
                .toList();
    }

    @Transactional
    public CategoryRow create(VerifiedAdmin admin, Map<String, String> names) {
        Map<String, String> validated = validate(names);
        String slug = uniqueSlug(validated.get("en"));
        CategoryEntity category = new CategoryEntity(slug);
        validated.forEach((language, name) -> category.addTranslation(new CategoryTranslation(category, language, name)));
        categories.save(category);
        audit.record(admin, "category.created", slug, summary(validated));
        categoryService.refresh();
        return toRow(category, Map.of());
    }

    @Transactional
    public CategoryRow update(VerifiedAdmin admin, String slug, Map<String, String> names) {
        Map<String, String> validated = validate(names);
        CategoryEntity category = categories.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        for (CategoryTranslation translation : category.getTranslations()) {
            String name = validated.get(translation.getLanguage());
            if (name != null) {
                translation.setName(name);
            }
        }
        categories.save(category);
        audit.record(admin, "category.updated", slug, summary(validated));
        categoryService.refresh();
        return toRow(category, Map.of());
    }

    @Transactional
    public void delete(VerifiedAdmin admin, String slug) {
        CategoryEntity category = categories.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        long questionCount = questions.countByCategory(slug);
        if (questionCount > 0) {
            throw new CategoryInUseException(questionCount);
        }
        categories.delete(category);
        audit.record(admin, "category.deleted", slug, null);
        categoryService.refresh();
    }

    private CategoryRow toRow(CategoryEntity category, Map<String, Long> byLanguage) {
        Map<String, String> names = new TreeMap<>();
        for (CategoryTranslation translation : category.getTranslations()) {
            names.put(translation.getLanguage(), translation.getName());
        }
        long total = byLanguage.values().stream().mapToLong(Long::longValue).sum();
        return new CategoryRow(category.getSlug(), names, total, byLanguage);
    }

    private Map<String, String> validate(Map<String, String> names) {
        if (names == null) {
            throw new IllegalArgumentException("names are required");
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String language : LANGUAGES) {
            String name = names.get(language);
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("name for '" + language + "' is required");
            }
            String trimmed = name.trim();
            if (trimmed.length() > MAX_NAME_LENGTH) {
                throw new IllegalArgumentException("name for '" + language + "' exceeds " + MAX_NAME_LENGTH + " characters");
            }
            result.put(language, trimmed);
        }
        return result;
    }

    // Auto-derived stable key from the English name; checked for uniqueness against ALL categories (seeded too).
    private String uniqueSlug(String englishName) {
        String base = slugify(englishName);
        if (base.isEmpty()) {
            base = "category";
        }
        String candidate = base;
        int suffix = 2;
        while (categories.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private static String slugify(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
    }

    private static String summary(Map<String, String> names) {
        return names.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}
