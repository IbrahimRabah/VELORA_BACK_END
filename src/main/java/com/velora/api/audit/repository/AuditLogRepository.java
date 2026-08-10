package com.velora.api.audit.repository;

import com.velora.api.audit.domain.AuditAction;
import com.velora.api.audit.domain.AuditLog;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action, Pageable pageable);

    Page<AuditLog> findByActorIdOrderByCreatedAtDesc(Long actorId, Pageable pageable);

    /** Everything that ever happened to one thing — the usual investigation. */
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, String entityId, Pageable pageable);

    @Query("""
            select a from AuditLog a
            where a.createdAt >= :from and a.createdAt < :to
            order by a.createdAt desc
            """)
    Page<AuditLog> findBetween(@Param("from") OffsetDateTime from,
                               @Param("to") OffsetDateTime to,
                               Pageable pageable);
}
