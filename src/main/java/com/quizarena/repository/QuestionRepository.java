package com.quizarena.repository;

import com.quizarena.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query(value = """
            SELECT * FROM questions
            WHERE language = :language
              AND (:category = '' OR category = :category)
              AND (:difficulty = '' OR difficulty = :difficulty)
            ORDER BY RANDOM() LIMIT :limit
            """, nativeQuery = true)
    List<Question> findRandomFiltered(@Param("category") String category,
                                      @Param("difficulty") String difficulty,
                                      @Param("language") String language,
                                      @Param("limit") int limit);

    @Query(value = """
            SELECT COUNT(*) FROM questions
            WHERE language = :language
              AND (:category = '' OR category = :category)
              AND (:difficulty = '' OR difficulty = :difficulty)
            """, nativeQuery = true)
    long countFiltered(@Param("category") String category, @Param("difficulty") String difficulty,
                       @Param("language") String language);

    boolean existsByQuestionHash(String questionHash);

    @Query(value = """
            SELECT category FROM questions
            WHERE language = :language AND category IS NOT NULL
            GROUP BY category
            HAVING COUNT(*) >= :min
            """, nativeQuery = true)
    List<String> categoriesWithMinQuestions(@Param("language") String language, @Param("min") int min);
}
