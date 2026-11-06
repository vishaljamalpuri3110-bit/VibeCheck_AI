package com.karasuno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Threaded comments and discussions between Creator, Manager, and Admin on a ContentSubmission.
 * Enables transparent collaboration and resolves missing message visibility.
 */
@Entity
@Table(name = "submission_comments", indexes = {
    @Index(name = "idx_comm_sub", columnList = "submissionId"),
    @Index(name = "idx_comm_date", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(nullable = false)
    private Long authorUserId;

    @Column(nullable = false, length = 100)
    private String authorUsername;

    @Column(nullable = false, length = 50)
    private String authorRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
