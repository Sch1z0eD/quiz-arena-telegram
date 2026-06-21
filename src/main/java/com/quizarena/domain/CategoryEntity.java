package com.quizarena.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String slug;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CategoryTranslation> translations = new ArrayList<>();

    protected CategoryEntity() {
    }

    public CategoryEntity(String slug) {
        this.slug = slug;
    }

    public void addTranslation(CategoryTranslation translation) {
        translations.add(translation);
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public List<CategoryTranslation> getTranslations() {
        return translations;
    }
}
