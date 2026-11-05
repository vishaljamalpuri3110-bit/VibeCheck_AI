package com.karasuno.backend.repository;

import com.karasuno.backend.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JPA repository for AuditLog — read-only access to the system audit trail. */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Audit events for a specific user, newest first */
    Page<AuditLog> findByActorUserIdOrderByOccurredAtDesc(Long userId, Pageable pageable);

    /** Audit events by action type */
    Page<AuditLog> findByActionOrderByOccurredAtDesc(String action, Pageable pageable);

    /** Audit trail for a specific entity instance */
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
            String entityType, Long entityId, Pageable pageable);

    /** Full audit log, newest first */
    Page<AuditLog> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
