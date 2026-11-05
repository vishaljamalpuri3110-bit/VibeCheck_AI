package com.karasuno.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * Represents a single claim's verification result from the AI compliance engine.
 * Maps directly from the strict JSON schema enforced in the OpenRouter system prompt.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerificationItem {

    /** The original sentence or claim being evaluated */
    private String claim;

    /** Tri-state result: SUPPORTED, CONTRADICTED, or UNVERIFIED */
    private String state;

    /** Confidence score between 0.0 and 1.0 */
    private Double confidenceScore;

    /** Evidence references linking back to source document chunks */
    private List<EvidenceReference> evidenceIds;

    /** Surgical correction suggestion — null if not applicable */
    private String suggestedCorrection;

    /** Short explanation of why this state was assigned */
    private String rationale;

    /** Independent validation result for the suggested correction */
private String correctionValidationState;

/** Explanation of why the correction passed or needs review */
private String correctionValidationRationale;
    /** Evidence citation record pointing to a specific document chunk. */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EvidenceReference {
        private Long chunkId;
        private String documentName;
        private Integer pageNumber;
        private String excerpt;
    }
}
