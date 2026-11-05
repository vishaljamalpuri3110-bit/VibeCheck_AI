package com.karasuno.backend.dto;

import com.karasuno.backend.entity.DraftReviewLog;
import lombok.Data;

/** Request body for POST /api/review/{id}/decision — manager's review decision. */
@Data
public class ReviewDecisionRequest {
    private DraftReviewLog.ReviewStatus decision;
    private String comments;
}
