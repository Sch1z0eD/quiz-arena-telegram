package com.quizarena.repository;

import com.quizarena.domain.AnswerRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnswerRepository extends JpaRepository<AnswerRecord, Long> {

    long countByUserId(long userId);

    long countByUserIdAndCorrectTrue(long userId);

    long countByCorrectTrue();

    @Query("SELECT COUNT(DISTINCT a.gameId) FROM AnswerRecord a WHERE a.userId = :userId")
    long countDistinctGamesByUserId(@Param("userId") long userId);

    long countByGameIdAndUserId(long gameId, long userId);

    long countByGameIdAndUserIdAndCorrectTrue(long gameId, long userId);

    long countByQuestionId(long questionId);

    long countByQuestionIdAndCorrectTrue(long questionId);

    @Query("SELECT COUNT(DISTINCT a.userId) FROM AnswerRecord a")
    long countDistinctUsers();

    @Query("SELECT COUNT(DISTINCT a.userId) FROM AnswerRecord a WHERE a.answeredAt >= :since")
    long countDistinctUsersSince(@Param("since") long since);

    @Query(value = "SELECT COUNT(DISTINCT game_id) FROM answers WHERE mode = 'GAME' AND chat_id > 0", nativeQuery = true)
    long countSoloGames();

    @Query(value = "SELECT COUNT(DISTINCT game_id) FROM answers WHERE mode = 'GAME' AND chat_id < 0", nativeQuery = true)
    long countGroupGames();

    @Query(value = """
            SELECT to_char(to_timestamp(answered_at / 1000) AT TIME ZONE 'UTC', 'YYYY-MM-DD') AS day,
                   COUNT(*) AS count
            FROM answers
            WHERE answered_at >= :since
            GROUP BY day
            ORDER BY day
            """, nativeQuery = true)
    List<DayCount> answersPerDay(@Param("since") long since);

    @Query(value = """
            SELECT q.category AS category, COUNT(*) AS count
            FROM answers a JOIN questions q ON q.id = a.question_id
            GROUP BY q.category
            ORDER BY count DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<CategoryAnswerCount> topCategoriesByAnswers(@Param("limit") int limit);

    @Query(value = """
            SELECT q.category AS category, COUNT(*) AS answered,
                   SUM(CASE WHEN a.correct THEN 1 ELSE 0 END) AS correct
            FROM answers a JOIN questions q ON q.id = a.question_id
            WHERE a.user_id = :userId
            GROUP BY q.category
            ORDER BY answered DESC
            """, nativeQuery = true)
    List<UserCategoryRow> userCategoryBreakdown(@Param("userId") long userId);

    @Query(value = """
            SELECT game_id AS gameId, mode AS mode, MAX(answered_at) AS finishedAt,
                   COUNT(*) AS total, SUM(CASE WHEN correct THEN 1 ELSE 0 END) AS correct
            FROM answers
            WHERE user_id = :userId
            GROUP BY game_id, mode
            ORDER BY finishedAt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<UserGameRow> recentGames(@Param("userId") long userId, @Param("limit") int limit);

    interface DayCount {
        String getDay();

        long getCount();
    }

    interface UserCategoryRow {
        String getCategory();

        long getAnswered();

        long getCorrect();
    }

    interface UserGameRow {
        long getGameId();

        String getMode();

        long getFinishedAt();

        long getTotal();

        long getCorrect();
    }

    interface CategoryAnswerCount {
        String getCategory();

        long getCount();
    }
}
