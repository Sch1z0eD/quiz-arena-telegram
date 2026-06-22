package com.quizarena.service;

import com.quizarena.domain.CategoryEntity;
import com.quizarena.domain.CategoryTranslation;
import com.quizarena.i18n.Localizer;
import com.quizarena.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Localized category display names backed by the database. Categories change rarely, so the full
 * slug -> language -> name map is cached in memory and reloaded only on {@link #refresh()}.
 */
@Service
public class CategoryService {

    private final CategoryRepository repository;
    private final Localizer localizer;
    private volatile Map<String, Cached> bySlug = Map.of();

    public CategoryService(CategoryRepository repository, Localizer localizer) {
        this.repository = repository;
        this.localizer = localizer;
    }

    @PostConstruct
    public void refresh() {
        Map<String, Cached> next = new HashMap<>();
        for (CategoryEntity category : repository.findAllWithTranslations()) {
            Map<String, String> byLanguage = new HashMap<>();
            for (CategoryTranslation translation : category.getTranslations()) {
                byLanguage.put(translation.getLanguage(), translation.getName());
            }
            next.put(category.getSlug(), new Cached(category.isActive(), byLanguage));
        }
        bySlug = Map.copyOf(next);
    }

    public String name(String slug, Locale locale) {
        Cached cached = bySlug.get(slug);
        if (cached == null) {
            return slug;
        }
        return cached.names().getOrDefault(locale.getLanguage(), slug);
    }

    // Display label for a (possibly absent) category filter: blank slug means "any".
    public String displayName(String slug, Locale locale) {
        return slug == null || slug.isEmpty() ? localizer.get(locale, "category.any") : name(slug, locale);
    }

    public boolean isEnabled(String slug) {
        Cached cached = bySlug.get(slug);
        return cached != null && cached.active();
    }

    private record Cached(boolean active, Map<String, String> names) {
    }
}
