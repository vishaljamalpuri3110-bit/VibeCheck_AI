package com.karasuno.backend.dto;

import com.karasuno.backend.entity.ContentSubmission;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubmissionDto {
    private Long id;
    private String title;
    private ContentSubmission.ContentType contentType;
    private Long creatorUserId;
    private String creatorUsername;
    private String content;
    private ContentSubmission.SubmissionStatus status;
    private Long latestDraftReviewLogId;
    private String overallState;
    private Integer voiceScore;
    private String voiceFeedback;
    private String creatorNote;
    private Long reviewedByUserId;
    private String reviewedByUsername;
    private String managerComments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private int commentCount;
}
