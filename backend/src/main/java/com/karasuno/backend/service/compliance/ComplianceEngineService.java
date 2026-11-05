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

/**
 * Module 3 — Hybrid Compliance Engine.
 *
 * <p>Full compliance pipeline for each draft submission:
 * <ol>
 *   <li><b>PII Masking</b> — DataPrivacyShieldService redacts PII locally.</li>
 *   <li><b>Local Rule Checks</b> — Deterministic banned keyword and regex scans.</li>
 *   <li><b>AI Analysis</b> — OpenRouter (Claude 3.5 Sonnet / Llama 3 fallback) for semantic verification.</li>
 *   <li><b>Response Parsing</b> — Strict JSON schema validation → tri-state VerificationItems.</li>
 *   <li><b>Persistence</b> — Full DraftReviewLog saved to MySQL.</li>
 * </ol>
 */
@Service
public class ComplianceEngineService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceEngineService.class);
    private static final int MAX_REFERENCE_CHUNKS = 20;

    private final DataPrivacyShieldService privacyShield;
    private final ComplianceRuleRepository complianceRuleRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DraftReviewLogRepository draftReviewLogRepository;
    private final OpenRouterClient openRouterClient;
    private final ObjectMapper objectMapper;

    public ComplianceEngineService(DataPrivacyShieldService privacyShield,
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

    /**
     * Executes the full compliance pipeline on a submitted draft.
     *
     * @param request              scan request containing draft text
     * @param submittedByUserId    authenticated user's ID
     * @param submittedByUsername  authenticated user's username
     * @return fully populated ComplianceScanResponse
     */
    @Transactional
    public ComplianceScanResponse scan(ComplianceScanRequest request,
                                       Long submittedByUserId,
                                       String submittedByUsername) {
        String originalText = request.getDraftText();
        log.info("Compliance scan started — user='{}', textLen={}", submittedByUsername, originalText.length());

        // ── STEP 1: PII Masking ──────────────────────────────────────────────
        DataPrivacyShieldService.MaskingResult maskResult = privacyShield.maskPii(originalText);
        String maskedText = maskResult.maskedText();

        // ── STEP 2: Local Deterministic Rule Checks ──────────────────────────
        List<ComplianceScanResponse.LocalViolation> localViolations = runLocalRules(maskedText);
        log.info("Local rules: {} violation(s)", localViolations.size());

        // ── STEP 3: Retrieve reference chunks for AI context ─────────────────
        List<DocumentChunk> refChunks = documentChunkRepository.findAll()
                .stream().limit(MAX_REFERENCE_CHUNKS).toList();
        log.info("Reference chunks available: {}", refChunks.size());

        // ── STEP 4: AI Semantic Analysis ─────────────────────────────────────
        List<VerificationItem> verificationItems = new ArrayList<>();
        String rawAiJson = null;

        try {
            rawAiJson = openRouterClient.analyzeCompliance(maskedText, refChunks);
            verificationItems = parseAiResponse(rawAiJson);
            log.info("AI analysis returned {} verification item(s)", verificationItems.size());
        } catch (Exception e) {
            log.error("AI analysis failed: {}", e.getMessage());
            // Graceful degradation: single UNVERIFIED item with error message
            VerificationItem fallback = new VerificationItem();
            fallback.setClaim(originalText.substring(0, Math.min(200, originalText.length())));
            fallback.setState("UNVERIFIED");
            fallback.setConfidenceScore(0.0);
            fallback.setRationale("AI analysis unavailable: " + e.getMessage());
            fallback.setEvidenceIds(List.of());
            verificationItems.add(fallback);
        }

        // ── STEP 5: Aggregate counts and determine overall state ─────────────
        int supported    = count(verificationItems, "SUPPORTED");
        int contradicted = count(verificationItems, "CONTRADICTED");
        int unverified   = count(verificationItems, "UNVERIFIED");
        DraftReviewLog.VerificationState overallState =
                resolveOverallState(supported, contradicted, unverified, localViolations.size());

        // ── STEP 6: Persist DraftReviewLog ───────────────────────────────────
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
                .reviewStatus(DraftReviewLog.ReviewStatus.PENDING_REVIEW)
                .build();
        DraftReviewLog saved = draftReviewLogRepository.save(reviewLog);

        // ── STEP 7: Build response ───────────────────────────────────────────
        ComplianceScanResponse response = new ComplianceScanResponse();
        response.setDraftReviewLogId(saved.getId());
        response.setOverallState(overallState);
        response.setVerificationItems(verificationItems);
        response.setLocalViolations(localViolations);
        response.setSupportedCount(supported);
        response.setContradictedCount(contradicted);
        response.setUnverifiedCount(unverified);
        response.setLocalViolationCount(localViolations.size());
        response.setPiiDetected(maskResult.piiDetected());
        response.setScannedAt(LocalDateTime.now());

        log.info("Scan complete — logId={}, state={}, S={}, C={}, U={}",
                saved.getId(), overallState, supported, contradicted, unverified);
        return response;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /** Runs all active local compliance rules against the text. */
    private List<ComplianceScanResponse.LocalViolation> runLocalRules(String text) {
        List<ComplianceScanResponse.LocalViolation> violations = new ArrayList<>();
        List<ComplianceRule> rules = complianceRuleRepository.findByActiveTrue();

        for (ComplianceRule rule : rules) {
            String matched = findMatch(text, rule);
            if (matched != null) {
                ComplianceScanResponse.LocalViolation v = new ComplianceScanResponse.LocalViolation();
                v.setRuleId(rule.getId());
                v.setRuleName(rule.getRuleName());
                v.setMatchedText(matched);
                v.setSeverity(rule.getSeverity().name());
                v.setDescription(rule.getDescription());
                violations.add(v);
            }
        }
        return violations;
    }

    /** Applies a single rule against text, returning the matched string or null. */
    private String findMatch(String text, ComplianceRule rule) {
        try {
            if (rule.getRuleType() == ComplianceRule.RuleType.BANNED_KEYWORD) {
                String lower = text.toLowerCase();
                String keyword = rule.getPattern().toLowerCase();
                int idx = lower.indexOf(keyword);
                if (idx >= 0) {
                    return text.substring(idx, Math.min(idx + keyword.length() + 20, text.length()));
                }
            } else {
                Matcher m = Pattern.compile(rule.getPattern(), Pattern.CASE_INSENSITIVE).matcher(text);
                if (m.find()) return m.group();
            }
        } catch (Exception e) {
            log.warn("Error evaluating rule '{}': {}", rule.getRuleName(), e.getMessage());
        }
        return null;
    }

    /** Parses the raw AI JSON response into VerificationItem list with schema validation. */
    private List<VerificationItem> parseAiResponse(String jsonResponse) {
        List<VerificationItem> items = new ArrayList<>();
        if (jsonResponse == null || jsonResponse.isBlank()) return items;

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode arr = root.path("verificationItems");
            if (!arr.isArray()) return items;

            for (JsonNode node : arr) {
                VerificationItem item = objectMapper.treeToValue(node, VerificationItem.class);
                // Validate and clamp confidence score
                if (item.getConfidenceScore() == null) item.setConfidenceScore(0.5);
                item.setConfidenceScore(Math.max(0.0, Math.min(1.0, item.getConfidenceScore())));
                // Enforce valid state
                if (!List.of("SUPPORTED", "CONTRADICTED", "UNVERIFIED").contains(item.getState())) {
                    item.setState("UNVERIFIED");
                }
                if (item.getEvidenceIds() == null) item.setEvidenceIds(List.of());
                items.add(item);
            }
        } catch (Exception e) {
            log.error("Failed to parse AI JSON response: {}", e.getMessage());
        }
        return items;
    }

    /** Counts items matching the given state string. */
    private int count(List<VerificationItem> items, String state) {
        return (int) items.stream().filter(v -> state.equals(v.getState())).count();
    }

    /**
     * Determines the overall draft state.
     * Any CONTRADICTED claim or local violation → CONTRADICTED.
     * All SUPPORTED and no UNVERIFIED → SUPPORTED.
     * Otherwise → UNVERIFIED.
     */
    private DraftReviewLog.VerificationState resolveOverallState(
            int supported, int contradicted, int unverified, int localViolations) {
        if (contradicted > 0 || localViolations > 0) return DraftReviewLog.VerificationState.CONTRADICTED;
        if (supported > 0 && unverified == 0)       return DraftReviewLog.VerificationState.SUPPORTED;
        return DraftReviewLog.VerificationState.UNVERIFIED;
    }
}
