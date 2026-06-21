package com.quizarena.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "category_translations")
@IdClass(CategoryTranslation.Key.class)
public class CategoryTranslation {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @Id
    @Column(name = "language", length = 8)
    private String language;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    protected CategoryTranslation() {
    }

    public String getLanguage() {
        return language;
    }

    public String getName() {
        return name;
    }

    public static class Key implements Serializable {
        private Long category;
        private String language;

        public Key() {
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return Objects.equals(category, key.category) && Objects.equals(language, key.language);
        }

        @Override
        public int hashCode() {
            return Objects.hash(category, language);
        }
    }
}
