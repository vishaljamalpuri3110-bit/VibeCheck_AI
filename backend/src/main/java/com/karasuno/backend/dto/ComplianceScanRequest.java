package com.karasuno.backend.dto;

import lombok.Data;

/** Request body for POST /api/compliance/scan. */
@Data
public class ComplianceScanRequest {
    /** The marketing copy or draft text to evaluate */
    private String draftText;
    /** Optional title for display in the review dashboard */
    private String draftTitle;
}
