package com.karasuno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Tracks content drafts and submissions throughout the Creator -> Manager -> Admin workflow.
 * Persists creator work across sessions, logouts, and page refreshes.
 */
@Entity
@Table(name = "content_submissions", indexes = {
    @Index(name = "idx_sub_creator", columnList = "creatorUserId"),
    @Index(name = "idx_sub_status", columnList = "status"),
    @Index(name = "idx_sub_date", columnList = "updatedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContentType contentType;

    @Column(nullable = false)
    private Long creatorUserId;

    @Column(nullable = false, length = 100)
    private String creatorUsername;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubmissionStatus status;

    /** Link to the most recent compliance scan result, if scanned */
    private Long latestDraftReviewLogId;

    /** Overall verification state cached from compliance scan */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DraftReviewLog.VerificationState overallState;

    /** Voice consistency score (0 - 100) */
    private Integer voiceScore;

    /** Explanation for voice score */
    @Column(columnDefinition = "TEXT")
    private String voiceFeedback;

    /** Full scan response JSON snapshot for instant restoration on reload */
    @Column(columnDefinition = "LONGTEXT")
    private String scanResponseSnapshot;

    /** Optional creator note sent to reviewers */
    @Column(columnDefinition = "TEXT")
    private String creatorNote;

    /** ID of manager or admin who made the review decision */
    private Long reviewedByUserId;

    /** Username of manager or admin reviewer */
    @Column(length = 100)
    private String reviewedByUsername;

    /** Manager/Admin review comments */
    @Column(columnDefinition = "TEXT")
    private String managerComments;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = SubmissionStatus.DRAFT;
        if (contentType == null) contentType = ContentType.GENERAL;
        if (title == null || title.isBlank()) title = "Untitled Draft";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ContentType {
        GENERAL, SOCIAL_POST, AD_COPY, EMAIL, BLOG_ARTICLE, PRESS_RELEASE
    }

    public enum SubmissionStatus {
        DRAFT, IN_REVIEW, APPROVED, REJECTED, NEEDS_REVISION
    }
}
