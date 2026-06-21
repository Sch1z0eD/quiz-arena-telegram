package com.quizarena.repository;

import com.quizarena.domain.DuelRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DuelRepository extends JpaRepository<DuelRecord, Long> {

    @Query(value = """
            SELECT COUNT(*) AS played,
                   COALESCE(SUM(CASE WHEN winner_id = :userId THEN 1 ELSE 0 END), 0) AS wins,
                   COALESCE(SUM(CASE WHEN winner_id IS NULL THEN 1 ELSE 0 END), 0) AS draws
            FROM duels
            WHERE user_a = :userId OR user_b = :userId
            """, nativeQuery = true)
    DuelRecordRow duelRecord(@Param("userId") long userId);

    interface DuelRecordRow {
        long getPlayed();

        long getWins();

        long getDraws();
    }
}
