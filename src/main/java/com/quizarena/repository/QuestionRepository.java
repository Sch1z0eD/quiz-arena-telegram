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
              AND active = TRUE
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
              AND active = TRUE
              AND (:category = '' OR category = :category)
              AND (:difficulty = '' OR difficulty = :difficulty)
            """, nativeQuery = true)
    long countFiltered(@Param("category") String category, @Param("difficulty") String difficulty,
                       @Param("language") String language);

    boolean existsByQuestionHash(String questionHash);

    long countByCategory(String category);

    @Query(value = """
            SELECT q.category FROM questions q
            JOIN categories c ON c.slug = q.category AND c.active = TRUE
            WHERE q.language = :language AND q.active = TRUE
            GROUP BY q.category
            HAVING COUNT(*) >= :min
            """, nativeQuery = true)
    List<String> categoriesWithMinQuestions(@Param("language") String language, @Param("min") int min);

    @Query(value = """
            SELECT difficulty FROM questions
            WHERE language = :language
              AND active = TRUE
              AND difficulty IS NOT NULL
              AND (:category = '' OR category = :category)
            GROUP BY difficulty
            HAVING COUNT(*) >= :min
            """, nativeQuery = true)
    List<String> difficultiesWithMinQuestions(@Param("category") String category,
                                              @Param("language") String language, @Param("min") int min);

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

    long countByActive(boolean active);

    @Query("SELECT q.category AS name, COUNT(q) AS count FROM Question q WHERE q.active = true GROUP BY q.category")
    List<NamedCount> activeCountByCategory();

    @Query("SELECT q.difficulty AS name, COUNT(q) AS count FROM Question q WHERE q.active = true GROUP BY q.difficulty")
    List<NamedCount> activeCountByDifficulty();

    @Query("SELECT q.language AS name, COUNT(q) AS count FROM Question q WHERE q.active = true GROUP BY q.language")
    List<NamedCount> activeCountByLanguage();

    @Query("""
            SELECT q.category AS category,
                   SUM(CASE WHEN q.correctOption = 0 THEN 1 ELSE 0 END) AS a,
                   SUM(CASE WHEN q.correctOption = 1 THEN 1 ELSE 0 END) AS b,
                   SUM(CASE WHEN q.correctOption = 2 THEN 1 ELSE 0 END) AS c,
                   SUM(CASE WHEN q.correctOption = 3 THEN 1 ELSE 0 END) AS d,
                   COUNT(q) AS total
            FROM Question q
            WHERE q.category IS NOT NULL
            GROUP BY q.category
            ORDER BY q.category
            """)
    List<CorrectPositionRow> correctOptionDistribution();

    interface CategoryLanguageCount {
        String getCategory();

        String getLanguage();

        long getCount();
    }

    interface CorrectPositionRow {
        String getCategory();

        long getA();

        long getB();

        long getC();

        long getD();

        long getTotal();
    }

    interface NamedCount {
        String getName();

        long getCount();
    }
}
