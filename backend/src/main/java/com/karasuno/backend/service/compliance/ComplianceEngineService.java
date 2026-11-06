
package com.karasuno.backend.service.compliance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.karasuno.backend.dto.ComplianceScanRequest;
import com.karasuno.backend.dto.ComplianceScanResponse;
import com.karasuno.backend.dto.VerificationItem;

import com.karasuno.backend.entity.ComplianceRule;
import com.karasuno.backend.entity.DocumentChunk;
import com.karasuno.backend.entity.DraftReviewLog;

import com.karasuno.backend.repository.ComplianceRuleRepository;
import com.karasuno.backend.repository.DocumentChunkRepository;
import com.karasuno.backend.repository.DraftReviewLogRepository;

import com.karasuno.backend.service.privacy.DataPrivacyShieldService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ComplianceEngineService {

    private static final Logger log =
            LoggerFactory.getLogger(ComplianceEngineService.class);

    private static final int MAX_REFERENCE_CHUNKS = 8;

    private static final String BRAND_HANDBOOK_TYPE =
            "BRAND_HANDBOOK";

    private final DataPrivacyShieldService privacyShield;
    private final ComplianceRuleRepository complianceRuleRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DraftReviewLogRepository draftReviewLogRepository;
    private final OpenRouterClient openRouterClient;
    private final ObjectMapper objectMapper;

    public ComplianceEngineService(
            DataPrivacyShieldService privacyShield,
            ComplianceRuleRepository complianceRuleRepository,
            DocumentChunkRepository documentChunkRepository,
            DraftReviewLogRepository draftReviewLogRepository,
            OpenRouterClient openRouterClient,
            ObjectMapper objectMapper) {

        this.privacyShield = privacyShield;
        this.complianceRuleRepository = complianceRuleRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.draftReviewLogRepository = draftReviewLogRepository;
        this.openRouterClient = openRouterClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ComplianceScanResponse scan(
            ComplianceScanRequest request,
            Long submittedByUserId,
            String submittedByUsername) {

        String originalText = request.getDraftText();

        if (originalText == null || originalText.isBlank()) {
            throw new IllegalArgumentException(
                    "Draft text cannot be empty."
            );
        }

        log.info(
                "Compliance scan started — user='{}', textLen={}",
                submittedByUsername,
                originalText.length()
        );

        // STEP 1: PII masking
        DataPrivacyShieldService.MaskingResult maskResult =
                privacyShield.maskPii(originalText);

        String maskedText = maskResult.maskedText();

        // STEP 2: Local deterministic rules
        List<ComplianceScanResponse.LocalViolation> localViolations =
                runLocalRules(maskedText);

        log.info(
                "Local rules: {} violation(s)",
                localViolations.size()
        );

        // STEP 3: Retrieve only brand handbook references
        List<DocumentChunk> refChunks = retrieveBrandReferences();

        log.info(
                "Brand handbook reference chunks available: {}",
                refChunks.size()
        );

        // STEP 4: AI analysis
        // Do not create fake UNVERIFIED findings when AI fails.
        String rawAiJson;

        try {
            rawAiJson = openRouterClient.analyzeCompliance(
                    maskedText,
                    refChunks
            );

        } catch (Exception e) {

            log.error(
                    "AI analysis failed. Scan cannot be completed: {}",
                    e.getMessage()
            );

            throw new IllegalStateException(
                    "Compliance analysis is temporarily unavailable. "
                            + "Please try again.",
                    e
            );
        }

List<VerificationItem> verificationItems =
        parseAiResponse(rawAiJson);

log.info(
        "AI analysis returned {} verification item(s)",
        verificationItems.size()
);

// STEP 4.5: Independently validate AI-generated corrections.
openRouterClient.validateCorrections(
        verificationItems,
        refChunks
);

        // STEP 5: Aggregate counts
        int supported = count(verificationItems, "SUPPORTED");
        int contradicted = count(verificationItems, "CONTRADICTED");
        int unverified = count(verificationItems, "UNVERIFIED");

        DraftReviewLog.VerificationState overallState =
                resolveOverallState(
                        supported,
                        contradicted,
                        unverified,
                        localViolations.size()
                );

        // STEP 6: Persist completed review
        DraftReviewLog reviewLog = DraftReviewLog.builder()
                .submittedByUserId(submittedByUserId)
                .submittedByUsername(submittedByUsername)
                .originalDraftText(originalText)
                .maskedDraftText(maskedText)
                .complianceResponseJson(rawAiJson)
                .overallState(overallState)
                .supportedCount(supported)
                .contradictedCount(contradicted)
                .unverifiedCount(unverified)
                .localViolationCount(localViolations.size())
                .reviewStatus(
                        DraftReviewLog.ReviewStatus.PENDING_REVIEW
                )
                .build();

        DraftReviewLog saved =
                draftReviewLogRepository.save(reviewLog);

        // STEP 7: Build response
        ComplianceScanResponse response =
                new ComplianceScanResponse();

        response.setDraftReviewLogId(saved.getId());
        response.setOverallState(overallState);
        response.setVerificationItems(verificationItems);
        response.setLocalViolations(localViolations);
        response.setSupportedCount(supported);
        response.setContradictedCount(contradicted);
        response.setUnverifiedCount(unverified);
        response.setLocalViolationCount(localViolations.size());
        response.setPiiDetected(maskResult.piiDetected());
        response.setAiDisclosure("Evaluated by VibeCheck AI Gemini Model. Independent human review is recommended before publishing.");
        response.setScannedAt(LocalDateTime.now());

        // Extract voice score if present in AI response
        try {
            JsonNode root = objectMapper.readTree(rawAiJson);
            if (root.has("voiceScore")) {
                response.setVoiceScore(root.path("voiceScore").asInt(90));
            } else {
                int score = Math.max(40, 100 - (contradicted * 25) - (unverified * 10) - (localViolations.size() * 15));
                response.setVoiceScore(score);
            }
            if (root.has("voiceFeedback")) {
                response.setVoiceFeedback(root.path("voiceFeedback").asText());
            } else {
                response.setVoiceFeedback(contradicted == 0 ? "Tone is consistent with brand voice guidelines." : "Contains phrases that contradict tone or terminology rules.");
            }
        } catch (Exception ignored) {
            response.setVoiceScore(85);
            response.setVoiceFeedback("Content evaluated against brand guidelines.");
        }

        log.info(
                "Scan complete — logId={}, state={}, S={}, C={}, U={}",
                saved.getId(),
                overallState,
                supported,
                contradicted,
                unverified
        );

        return response;
    }

    /**
     * Retrieves brand handbook chunks in document/page order.
     */
    private List<DocumentChunk> retrieveBrandReferences() {

        List<DocumentChunk> chunks =
                documentChunkRepository
                        .findByDocumentTypeOrderByDocumentNameAscPageNumberAsc(
                                BRAND_HANDBOOK_TYPE
                        );

        if (chunks == null || chunks.isEmpty()) {

            log.warn(
                    "No documents found with documentType={}",
                    BRAND_HANDBOOK_TYPE
            );

            return List.of();
        }

        if (chunks.size() > MAX_REFERENCE_CHUNKS) {

            log.warn(
                    "Brand reference contains {} chunks. "
                            + "Using first {} chunks.",
                    chunks.size(),
                    MAX_REFERENCE_CHUNKS
            );

            return new ArrayList<>(
                    chunks.subList(0, MAX_REFERENCE_CHUNKS)
            );
        }

        return chunks;
    }

    /**
     * Runs all active local compliance rules.
     */
    private List<ComplianceScanResponse.LocalViolation> runLocalRules(
            String text) {

        List<ComplianceScanResponse.LocalViolation> violations =
                new ArrayList<>();

        List<ComplianceRule> rules =
                complianceRuleRepository.findByActiveTrue();

        for (ComplianceRule rule : rules) {

            String matched = findMatch(text, rule);

            if (matched != null) {

                ComplianceScanResponse.LocalViolation violation =
                        new ComplianceScanResponse.LocalViolation();

                violation.setRuleId(rule.getId());
                violation.setRuleName(rule.getRuleName());
                violation.setMatchedText(matched);
                violation.setSeverity(rule.getSeverity().name());
                violation.setDescription(rule.getDescription());

                violations.add(violation);
            }
        }

        return violations;
    }

    /**
     * Applies a single local rule.
     */
    private String findMatch(String text, ComplianceRule rule) {

        try {

            if (rule.getRuleType()
                    == ComplianceRule.RuleType.BANNED_KEYWORD) {

                String lower = text.toLowerCase();
                String keyword = rule.getPattern().toLowerCase();

                int index = lower.indexOf(keyword);

                if (index >= 0) {

                    return text.substring(
                            index,
                            Math.min(
                                    index + keyword.length() + 20,
                                    text.length()
                            )
                    );
                }

            } else {

                Matcher matcher = Pattern.compile(
                        rule.getPattern(),
                        Pattern.CASE_INSENSITIVE
                ).matcher(text);

                if (matcher.find()) {
                    return matcher.group();
                }
            }

        } catch (Exception e) {

            log.warn(
                    "Error evaluating rule '{}': {}",
                    rule.getRuleName(),
                    e.getMessage()
            );
        }

        return null;
    }

    /**
     * Parses and validates AI JSON.
     */
    private List<VerificationItem> parseAiResponse(
            String jsonResponse) {

        if (jsonResponse == null || jsonResponse.isBlank()) {

            throw new IllegalStateException(
                    "AI returned an empty compliance response."
            );
        }

        try {

            JsonNode root =
                    objectMapper.readTree(jsonResponse);

            JsonNode array =
                    root.path("verificationItems");

            if (!array.isArray()) {

                throw new IllegalStateException(
                        "AI response contains no verification items."
                );
            }

            List<VerificationItem> items =
                    new ArrayList<>();

            for (JsonNode node : array) {

                VerificationItem item =
                        objectMapper.treeToValue(
                                node,
                                VerificationItem.class
                        );

                if (item.getClaim() == null
                        || item.getClaim().isBlank()) {

                    throw new IllegalStateException(
                            "AI returned a verification item without a claim."
                    );
                }

                if (item.getConfidenceScore() == null) {
                    item.setConfidenceScore(0.5);
                }

                item.setConfidenceScore(
                        Math.max(
                                0.0,
                                Math.min(
                                        1.0,
                                        item.getConfidenceScore()
                                )
                        )
                );

                String state = item.getState();

                if (state == null || !List.of(
                        "SUPPORTED",
                        "CONTRADICTED",
                        "UNVERIFIED"
                ).contains(state.toUpperCase())) {

                    throw new IllegalStateException(
                            "AI returned an invalid verification state."
                    );
                }

                item.setState(state.toUpperCase());

                if (item.getSeverity() == null || item.getSeverity().isBlank()) {
                    if ("CONTRADICTED".equals(item.getState())) {
                        item.setSeverity("CRITICAL");
                    } else if ("UNVERIFIED".equals(item.getState())) {
                        item.setSeverity("WARNING");
                    } else {
                        item.setSeverity("SUGGESTION");
                    }
                }

                if (item.getGuidelineReference() == null || item.getGuidelineReference().isBlank()) {
                    item.setGuidelineReference("Brand Communication Guidelines");
                }

                if (item.getFindingType() == null || item.getFindingType().isBlank()) {
                    item.setFindingType("CLAIM_VERIFICATION");
                }

                items.add(item);
            }

            return items;

        } catch (IllegalStateException e) {
            throw e;

        } catch (Exception e) {

            log.error(
                    "Failed to parse AI compliance JSON: {}",
                    e.getMessage()
            );

            throw new IllegalStateException(
                    "Could not parse AI compliance response.",
                    e
            );
        }
    }

    private int count(
            List<VerificationItem> items,
            String state) {

        return (int) items.stream()
                .filter(item -> state.equals(item.getState()))
                .count();
    }

    /**
     * Resolves the overall compliance state.
     */
    private DraftReviewLog.VerificationState resolveOverallState(
            int supported,
            int contradicted,
            int unverified,
            int localViolations) {

        if (contradicted > 0 || localViolations > 0) {
    return DraftReviewLog.VerificationState.CONTRADICTED;
}

if (unverified > 0) {
    return DraftReviewLog.VerificationState.UNVERIFIED;
}

return DraftReviewLog.VerificationState.SUPPORTED;
    }
}
