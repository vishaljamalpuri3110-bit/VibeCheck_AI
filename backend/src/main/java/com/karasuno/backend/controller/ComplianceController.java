package com.karasuno.backend.controller;

import com.karasuno.backend.dto.ComplianceScanRequest;
import com.karasuno.backend.dto.ComplianceScanResponse;
import com.karasuno.backend.entity.ComplianceRule;
import com.karasuno.backend.repository.ComplianceRuleRepository;
import com.karasuno.backend.repository.UserRepository;
import com.karasuno.backend.service.audit.AuditService;
import com.karasuno.backend.service.compliance.ComplianceEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for compliance scanning and rule management.
 * Core endpoint: POST /api/compliance/scan triggers the full hybrid compliance pipeline.
 */
@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    private final ComplianceEngineService complianceEngineService;
    private final ComplianceRuleRepository complianceRuleRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final com.karasuno.backend.service.voice.BrandVoiceService brandVoiceService;

    public ComplianceController(ComplianceEngineService complianceEngineService,
                                ComplianceRuleRepository complianceRuleRepository,
                                UserRepository userRepository,
                                AuditService auditService,
                                com.karasuno.backend.service.voice.BrandVoiceService brandVoiceService) {
        this.complianceEngineService = complianceEngineService;
        this.complianceRuleRepository = complianceRuleRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.brandVoiceService = brandVoiceService;
    }

    /**
     * "Write in our voice" assistance workflow.
     * Uses past approved examples and brand guidelines to generate voice-consistent drafting suggestions.
     */
    @PostMapping("/voice-assist")
    public ResponseEntity<?> writeInOurVoice(
            @RequestBody java.util.Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String prompt = request.getOrDefault("prompt", "");
        String contentType = request.getOrDefault("contentType", "GENERAL");
        if (prompt.isBlank()) {
            return ResponseEntity.badRequest().body("Prompt is required.");
        }
        return ResponseEntity.ok(brandVoiceService.writeInOurVoice(prompt, contentType));
    }

    /**
     * Main compliance scan endpoint.
     * Pipeline: PII mask → local rules → AI analysis → persist → respond.
     */
    @PostMapping("/scan")
    public ResponseEntity<ComplianceScanResponse> scan(
            @RequestBody ComplianceScanRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (request.getDraftText() == null || request.getDraftText().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Long userId = userRepository.findByUsername(userDetails.getUsername())
                .map(u -> u.getId()).orElse(0L);

        ComplianceScanResponse response = complianceEngineService.scan(
                request, userId, userDetails.getUsername());

        auditService.recordAsync(userId, userDetails.getUsername(), "COMPLIANCE_SCAN",
                "DraftReviewLog", response.getDraftReviewLogId(),
                "Overall: " + response.getOverallState(), null);

        return ResponseEntity.ok(response);
    }

    /** Lists all currently active compliance rules. */
    @GetMapping("/rules")
    public ResponseEntity<List<ComplianceRule>> getRules() {
        return ResponseEntity.ok(complianceRuleRepository.findByActiveTrue());
    }

    /** Creates a new compliance rule. */
    @PostMapping("/rules")
    public ResponseEntity<ComplianceRule> createRule(
            @RequestBody ComplianceRule rule,
            @AuthenticationPrincipal UserDetails userDetails) {
        ComplianceRule saved = complianceRuleRepository.save(rule);
        auditService.recordAsync(null, userDetails.getUsername(), "RULE_CREATED",
                "ComplianceRule", saved.getId(), "Rule: " + saved.getRuleName(), null);
        return ResponseEntity.ok(saved);
    }

    /** Soft-deactivates a compliance rule (sets active = false). */
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<?> deactivateRule(@PathVariable Long id,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        return complianceRuleRepository.findById(id).map(rule -> {
            rule.setActive(false);
            complianceRuleRepository.save(rule);
            auditService.recordAsync(null, userDetails.getUsername(), "RULE_DEACTIVATED",
                    "ComplianceRule", id, "Rule: " + rule.getRuleName(), null);
            return ResponseEntity.ok("Rule '" + rule.getRuleName() + "' deactivated.");
        }).orElse(ResponseEntity.notFound().build());
    }
}
