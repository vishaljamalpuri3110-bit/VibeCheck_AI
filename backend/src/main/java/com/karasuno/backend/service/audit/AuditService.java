package com.karasuno.backend.service.audit;

import com.karasuno.backend.entity.AuditLog;
import com.karasuno.backend.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for creating and querying the immutable system audit trail.
 * Write operations are @Async to avoid adding latency to request threads.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Records a new audit event asynchronously (fire-and-forget).
     * The calling thread returns immediately; the write happens in the background.
     */
    @Async
    public void recordAsync(Long actorUserId, String actorUsername, String action,
                            String entityType, Long entityId, String details, String ipAddress) {
        AuditLog entry = AuditLog.builder()
                .actorUserId(actorUserId != null ? actorUserId : 0L)
                .actorUsername(actorUsername != null ? actorUsername : "system")
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(entry);
    }

    /** All audit logs, newest first, paginated. */
    public Page<AuditLog> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByOccurredAtDesc(pageable);
    }

    /** Audit logs for a specific user. */
    public Page<AuditLog> getLogsForUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByActorUserIdOrderByOccurredAtDesc(userId, pageable);
    }

    /** Audit logs for a specific entity instance. */
    public Page<AuditLog> getLogsForEntity(String entityType, Long entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
                entityType, entityId, pageable);
    }
}
