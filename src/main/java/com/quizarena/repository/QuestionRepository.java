package com.quizarena.repository;

import com.quizarena.domain.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("""
            SELECT q FROM Question q
            WHERE (:text = '' OR LOWER(q.text) LIKE LOWER(CONCAT('%', :text, '%')))
              AND (:category = '' OR q.category = :category)
              AND (:difficulty = '' OR q.difficulty = :difficulty)
              AND (:language = '' OR q.language = :language)
            """)
    Page<Question> search(@Param("text") String text, @Param("category") String category,
                          @Param("difficulty") String difficulty, @Param("language") String language,
                          Pageable pageable);

    @Query("""
            SELECT q.category AS category, q.language AS language, COUNT(q) AS count
            FROM Question q
            WHERE q.category IS NOT NULL
            GROUP BY q.category, q.language
            """)
    List<CategoryLanguageCount> categoryCounts();

    interface CategoryLanguageCount {
        String getCategory();

        String getLanguage();

        long getCount();
    }
}
