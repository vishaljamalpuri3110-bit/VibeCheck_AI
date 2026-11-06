package com.karasuno.backend.dto;

import com.karasuno.backend.entity.DraftReviewLog;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/** Full compliance scan response returned to the Angular frontend. */
@Data
public class ComplianceScanResponse {

    /** DB primary key of the persisted DraftReviewLog record */
    private Long draftReviewLogId;

    /** Overall verdict for the entire draft */
    private DraftReviewLog.VerificationState overallState;

    /** Per-claim AI verification results */
    private List<VerificationItem> verificationItems;

    /** Local deterministic rule violations (found before AI call) */
    private List<LocalViolation> localViolations;

    private int supportedCount;
    private int contradictedCount;
    private int unverifiedCount;
    private int localViolationCount;

    /** True if PII was detected and masked before analysis */
    private boolean piiDetected;

    /** Brand Voice Consistency Score: 0 to 100 */
    private Integer voiceScore;

    /** Concise explanation of brand voice score */
    private String voiceFeedback;

    /** AI engine disclosure notice */
    private String aiDisclosure;

    private LocalDateTime scannedAt;

    /** A single local rule violation record. */
    @Data
    public static class LocalViolation {
        private Long ruleId;
        private String ruleName;
        private String matchedText;
        private String severity;
        private String description;
    }
}
