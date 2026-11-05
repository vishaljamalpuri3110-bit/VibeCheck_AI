package com.karasuno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Records the full compliance evaluation result for every submitted marketing draft.
 * Stores the original text, PII-masked text, raw AI response JSON,
 * tri-state verification outcome, and manager review state.
 */
@Entity
@Table(name = "draft_review_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID of the user who submitted this draft */
    @Column(nullable = false)
    private Long submittedByUserId;

    /** Username for quick display (denormalized) */
    @Column(nullable = false, length = 100)
    private String submittedByUsername;

    /** Original draft text submitted by the content creator */
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String originalDraftText;

    /** PII-masked version of the draft sent to the AI */
    @Column(columnDefinition = "MEDIUMTEXT")
    private String maskedDraftText;

    /** Raw JSON compliance response from OpenRouter */
    @Column(columnDefinition = "LONGTEXT")
    private String complianceResponseJson;

    /** Overall verification state: SUPPORTED, CONTRADICTED, or UNVERIFIED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationState overallState;

    private Integer supportedCount;
    private Integer contradictedCount;
    private Integer unverifiedCount;
    private Integer localViolationCount;

    /** Current manager review status */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus reviewStatus;

    /** ID of the manager who reviewed (null if still pending) */
    private Long reviewedByManagerId;

    /** Manager's comments on the review decision */
    @Column(columnDefinition = "TEXT")
    private String managerComments;

    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        if (reviewStatus == null) reviewStatus = ReviewStatus.PENDING_REVIEW;
    }

    public enum VerificationState {
        SUPPORTED, CONTRADICTED, UNVERIFIED
    }

    public enum ReviewStatus {
        PENDING_REVIEW, APPROVED, REJECTED, NEEDS_REVISION
    }
}
