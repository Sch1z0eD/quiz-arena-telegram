package com.quizarena.repository;

import com.quizarena.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO users (id, name, username, first_seen, last_seen, blocked)
            VALUES (:id, :name, :username, :now, :now, FALSE)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                username = EXCLUDED.username,
                last_seen = EXCLUDED.last_seen,
                blocked = FALSE
            """, nativeQuery = true)
    void touch(@Param("id") long id, @Param("name") String name, @Param("username") String username,
               @Param("now") long now);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.name = :name WHERE u.id = :id AND u.name IS NULL")
    int backfillNameIfMissing(@Param("id") long id, @Param("name") String name);
}
