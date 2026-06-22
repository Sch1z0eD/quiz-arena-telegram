package com.quizarena.repository;

import com.quizarena.domain.Broadcast;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BroadcastRepository extends JpaRepository<Broadcast, Long> {

    Page<Broadcast> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByStatus(String status);

    // Atomic gate: only one starter wins the DRAFT->RUNNING flip for a given token, so a double start
    // cannot launch two engines on one broadcast. Also pins total to the live recipient count.
    @Modifying
    @Transactional
    @Query("UPDATE Broadcast b SET b.status = 'RUNNING', b.total = :total "
            + "WHERE b.id = :id AND b.status = 'DRAFT' AND b.confirmToken = :token")
    int startIfDraft(@Param("id") long id, @Param("token") String token, @Param("total") int total);

    @Modifying
    @Transactional
    @Query("UPDATE Broadcast b SET b.sent = :sent, b.failed = :failed WHERE b.id = :id")
    void updateProgress(@Param("id") long id, @Param("sent") int sent, @Param("failed") int failed);

    @Modifying
    @Transactional
    @Query("UPDATE Broadcast b SET b.status = :status, b.sent = :sent, b.failed = :failed WHERE b.id = :id")
    void finish(@Param("id") long id, @Param("status") String status, @Param("sent") int sent, @Param("failed") int failed);

    @Modifying
    @Transactional
    @Query("UPDATE Broadcast b SET b.status = 'INTERRUPTED' WHERE b.status = 'RUNNING'")
    int markRunningInterrupted();
}
