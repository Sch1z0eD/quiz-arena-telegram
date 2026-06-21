package com.quizarena.service;

import com.quizarena.domain.CategoryEntity;
import com.quizarena.domain.CategoryTranslation;
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
    private volatile Map<String, Map<String, String>> namesBySlug = Map.of();

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void refresh() {
        Map<String, Map<String, String>> next = new HashMap<>();
        for (CategoryEntity category : repository.findAllWithTranslations()) {
            Map<String, String> byLanguage = new HashMap<>();
            for (CategoryTranslation translation : category.getTranslations()) {
                byLanguage.put(translation.getLanguage(), translation.getName());
            }
            next.put(category.getSlug(), byLanguage);
        }
        namesBySlug = Map.copyOf(next);
    }

    public String name(String slug, Locale locale) {
        Map<String, String> byLanguage = namesBySlug.get(slug);
        if (byLanguage == null) {
            return slug;
        }
        return byLanguage.getOrDefault(locale.getLanguage(), slug);
    }
}
