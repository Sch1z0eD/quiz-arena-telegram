package com.quizarena.repository;

import com.quizarena.domain.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    @Query("SELECT DISTINCT c FROM CategoryEntity c LEFT JOIN FETCH c.translations")
    List<CategoryEntity> findAllWithTranslations();

    @Query("SELECT DISTINCT c FROM CategoryEntity c LEFT JOIN FETCH c.translations WHERE c.slug = :slug")
    Optional<CategoryEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
