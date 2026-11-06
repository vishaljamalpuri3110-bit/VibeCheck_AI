package com.karasuno.backend.dto;

import com.karasuno.backend.entity.ContentSubmission;
import lombok.Data;

@Data
public class SaveDraftRequest {
    private Long id;
    private String title;
    private ContentSubmission.ContentType contentType;
    private String content;
    private Long latestDraftReviewLogId;
    private String overallState;
    private Integer voiceScore;
    private String voiceFeedback;
    private String scanResponseSnapshot;
    private String creatorNote;
}
