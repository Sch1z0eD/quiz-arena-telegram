package com.quizarena.repository;

import com.quizarena.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {

    // List: aggregate answers across ALL users first, then LEFT JOIN so contacted-but-never-played users
    // (games = 0) still appear. "games" counts solo/group quiz games only; duels share the game_id
    // namespace, so a blanket COUNT(DISTINCT game_id) would both include duels and collide with them.
    @Query(value = """
            SELECT u.id AS id, u.name AS name, u.username AS username, s.language AS language,
                   COALESCE(a.games, 0) AS games,
                   CASE WHEN COALESCE(a.answered, 0) = 0 THEN NULL ELSE a.correct * 100.0 / a.answered END AS accuracy,
                   COALESCE(r.elo, 1000) AS elo,
                   u.first_seen AS firstSeen, u.last_seen AS lastSeen, u.banned AS banned, u.blocked AS blocked
            FROM users u
            LEFT JOIN (
                SELECT user_id,
                       COUNT(DISTINCT game_id) FILTER (WHERE mode = 'GAME') AS games,
                       COUNT(*) AS answered,
                       SUM(CASE WHEN correct THEN 1 ELSE 0 END) AS correct
                FROM answers GROUP BY user_id
            ) a ON a.user_id = u.id
            LEFT JOIN user_rating r ON r.user_id = u.id
            LEFT JOIN user_settings s ON s.user_id = u.id
            WHERE (:search = '' OR u.name ILIKE CONCAT('%', :search, '%') OR u.id = :searchId)
            """,
            countQuery = """
            SELECT COUNT(*) FROM users u
            WHERE (:search = '' OR u.name ILIKE CONCAT('%', :search, '%') OR u.id = :searchId)
            """,
            nativeQuery = true)
    Page<UserRowProjection> searchUsers(@Param("search") String search, @Param("searchId") long searchId, Pageable pageable);

    // Detail header: same shape, but the inner aggregate is scoped to this user (idx_answers_user) instead of
    // a full GROUP BY filtered at the end - one user's open must not trigger an all-users aggregation.
    @Query(value = """
            SELECT u.id AS id, u.name AS name, u.username AS username, s.language AS language,
                   COALESCE(a.games, 0) AS games,
                   CASE WHEN COALESCE(a.answered, 0) = 0 THEN NULL ELSE a.correct * 100.0 / a.answered END AS accuracy,
                   COALESCE(r.elo, 1000) AS elo,
                   u.first_seen AS firstSeen, u.last_seen AS lastSeen, u.banned AS banned, u.blocked AS blocked
            FROM users u
            LEFT JOIN (
                SELECT user_id,
                       COUNT(DISTINCT game_id) FILTER (WHERE mode = 'GAME') AS games,
                       COUNT(*) AS answered,
                       SUM(CASE WHEN correct THEN 1 ELSE 0 END) AS correct
                FROM answers WHERE user_id = :id GROUP BY user_id
            ) a ON a.user_id = u.id
            LEFT JOIN user_rating r ON r.user_id = u.id
            LEFT JOIN user_settings s ON s.user_id = u.id
            WHERE u.id = :id
            """, nativeQuery = true)
    Optional<UserRowProjection> findUserHeader(@Param("id") long id);

    interface UserRowProjection {
        long getId();

        String getName();

        String getUsername();

        String getLanguage();

        long getGames();

        Double getAccuracy();

        int getElo();

        long getFirstSeen();

        long getLastSeen();

        boolean getBanned();

        boolean getBlocked();
    }

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.name = :name WHERE u.id = :id AND u.name IS NULL")
    int backfillNameIfMissing(@Param("id") long id, @Param("name") String name);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.banned = :banned WHERE u.id = :id")
    int setBanned(@Param("id") long id, @Param("banned") boolean banned);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.blocked = true WHERE u.id = :id")
    void markBlocked(@Param("id") long id);

    // Broadcast recipients: registry minus banned and blocked; language '' means all (language lives in
    // user_settings, so we LEFT JOIN it). Resolved live at start, not snapshotted at dry-run.
    @Query(value = """
            SELECT COUNT(*) FROM users u
            LEFT JOIN user_settings s ON s.user_id = u.id
            WHERE u.banned = FALSE AND u.blocked = FALSE AND (:language = '' OR s.language = :language)
            """, nativeQuery = true)
    long countRecipients(@Param("language") String language);

    @Query(value = """
            SELECT u.id FROM users u
            LEFT JOIN user_settings s ON s.user_id = u.id
            WHERE u.banned = FALSE AND u.blocked = FALSE AND (:language = '' OR s.language = :language)
            ORDER BY u.id
            """, nativeQuery = true)
    List<Long> findRecipientIds(@Param("language") String language);
}
