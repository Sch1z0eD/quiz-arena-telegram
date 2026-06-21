package com.quizarena.repository;

import com.quizarena.domain.AnswerRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnswerRepository extends JpaRepository<AnswerRecord, Long> {

    long countByUserId(long userId);

    long countByUserIdAndCorrectTrue(long userId);

    @Query("SELECT COUNT(DISTINCT a.gameId) FROM AnswerRecord a WHERE a.userId = :userId")
    long countDistinctGamesByUserId(@Param("userId") long userId);

    long countByGameIdAndUserId(long gameId, long userId);

    long countByGameIdAndUserIdAndCorrectTrue(long gameId, long userId);

    long countByQuestionId(long questionId);

    long countByQuestionIdAndCorrectTrue(long questionId);
}
