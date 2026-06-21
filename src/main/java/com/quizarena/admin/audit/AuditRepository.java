package com.quizarena.admin.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditRepository extends JpaRepository<AuditEntity, Long> {

    @Query("""
            SELECT e FROM AuditEntity e
            WHERE (:action = '' OR e.action = :action)
              AND (:adminId = 0 OR e.adminId = :adminId)
              AND (:target = '' OR LOWER(e.target) LIKE LOWER(CONCAT('%', :target, '%')))
              AND e.ts >= :from
              AND e.ts <= :to
            """)
    Page<AuditEntity> search(@Param("action") String action, @Param("adminId") long adminId,
                             @Param("target") String target, @Param("from") Instant from,
                             @Param("to") Instant to, Pageable pageable);

    @Query("SELECT DISTINCT e.action FROM AuditEntity e ORDER BY e.action")
    List<String> distinctActions();
}
