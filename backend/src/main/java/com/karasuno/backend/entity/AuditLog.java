package com.karasuno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Immutable audit trail for all significant system actions.
 * Records are only INSERTed — never updated — to provide a tamper-evident history.
 * Indexed for efficient filtering by user, action type, and timestamp.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "actorUserId"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_time", columnList = "occurredAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID of the user who performed the action */
    @Column(nullable = false)
    private Long actorUserId;

    @Column(nullable = false, length = 100)
    private String actorUsername;

    /**
     * Action type constant, e.g.:
     * PDF_UPLOAD, COMPLIANCE_SCAN, DRAFT_SUBMIT,
     * MANAGER_APPROVE, MANAGER_REJECT, USER_LOGIN, RULE_CREATED
     */
    @Column(nullable = false, length = 100)
    private String action;

    /** Entity type affected, e.g. "DraftReviewLog", "DocumentChunk" */
    @Column(length = 100)
    private String entityType;

    /** Primary key of the affected entity */
    private Long entityId;

    /** Additional context as a JSON string */
    @Column(columnDefinition = "TEXT")
    private String details;

    /** Client IP address (IPv4 or IPv6) */
    @Column(length = 45)
    private String ipAddress;

    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    protected void onCreate() {
        occurredAt = LocalDateTime.now();
    }
}
