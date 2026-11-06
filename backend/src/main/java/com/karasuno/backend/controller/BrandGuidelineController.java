package com.karasuno.backend.controller;

import com.karasuno.backend.entity.BrandFactClaim;
import com.karasuno.backend.entity.BrandGuideline;
import com.karasuno.backend.entity.BrandTerm;
import com.karasuno.backend.repository.BrandFactClaimRepository;
import com.karasuno.backend.repository.BrandGuidelineRepository;
import com.karasuno.backend.repository.BrandTermRepository;
import com.karasuno.backend.service.audit.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/guidelines")
public class BrandGuidelineController {

    private final BrandGuidelineRepository guidelineRepository;
    private final BrandTermRepository termRepository;
    private final BrandFactClaimRepository factClaimRepository;
    private final AuditService auditService;

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");

    public BrandGuidelineController(BrandGuidelineRepository guidelineRepository,
                                    BrandTermRepository termRepository,
                                    BrandFactClaimRepository factClaimRepository,
                                    AuditService auditService) {
        this.guidelineRepository = guidelineRepository;
        this.termRepository = termRepository;
        this.factClaimRepository = factClaimRepository;
        this.auditService = auditService;
    }

    /**
     * Retrieves the active brand guideline profile. Accessible to all authenticated users.
     */
    @GetMapping("/active")
    public ResponseEntity<BrandGuideline> getActiveGuideline() {
        return guidelineRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    // Seed initial default guideline if none exists
                    BrandGuideline initial = BrandGuideline.builder()
                            .brandName("VibeCheck AI")
                            .version("1.0")
                            .missionStatement("Deliver thoughtful, reliable, and intelligent brand governance.")
                            .targetAudience("Content creators, brand managers, and marketing teams.")
                            .toneOfVoice("Calm, trustworthy, professional, clear, empathetic.")
                            .primaryColors("#F7F5F1,#FFFFFF,#292824,#A9664D")
                            .competitorWatchlist("CompetitorX, RivalBrand, CopyCatAI")
                            .guidelineContent("Maintain clear, honest communication without hype, superlatives, or unverified claims.")
                            .active(true)
                            .build();
                    return ResponseEntity.ok(guidelineRepository.save(initial));
                });
    }

    /**
     * Lists guideline version history (Manager/Admin).
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<BrandGuideline>> getGuidelineHistory() {
        return ResponseEntity.ok(guidelineRepository.findAllByOrderByCreatedAtDesc());
    }

    /**
     * Saves or updates brand guidelines (Builder workflow, Manager/Admin).
     * Validates hex color values before saving.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<?> saveGuideline(
            @RequestBody BrandGuideline guideline,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Validate hex color codes if provided
        if (guideline.getPrimaryColors() != null && !guideline.getPrimaryColors().isBlank()) {
            String[] colors = guideline.getPrimaryColors().split(",");
            for (String color : colors) {
                String c = color.trim();
                if (!c.isBlank() && !HEX_COLOR_PATTERN.matcher(c).matches()) {
                    return ResponseEntity.badRequest().body("Invalid hex color format: '" + c + "'. Use format #RRGGBB or #RGB.");
                }
            }
        }

        if (guideline.getBrandName() == null || guideline.getBrandName().isBlank()) {
            guideline.setBrandName("VibeCheck AI");
        }

        BrandGuideline saved = guidelineRepository.save(guideline);
        auditService.recordAsync(null, userDetails.getUsername(), "GUIDELINE_SAVED",
                "BrandGuideline", saved.getId(), "Version: " + saved.getVersion(), null);

        return ResponseEntity.ok(saved);
    }

    /**
     * Terminology & Glossary: list active terms.
     */
    @GetMapping("/terms")
    public ResponseEntity<List<BrandTerm>> getTerms() {
        List<BrandTerm> terms = termRepository.findByActiveTrue();
        if (terms.isEmpty()) {
            // Seed initial common terms
            List<BrandTerm> defaultTerms = List.of(
                    BrandTerm.builder().term("synergy").prohibited(true).approvedAlternative("collaboration").category("PROMOTIONAL_HYPE").rationale("Overused corporate jargon.").active(true).build(),
                    BrandTerm.builder().term("guaranteed 100%").prohibited(true).approvedAlternative("designed for high reliability").category("LEGAL").rationale("Absolute promises create legal risk.").active(true).build(),
                    BrandTerm.builder().term("revolutionary").prohibited(true).approvedAlternative("innovative").category("PROMOTIONAL_HYPE").rationale("Avoid exaggerated superlatives.").active(true).build(),
                    BrandTerm.builder().term("VibeCheck AI").prohibited(false).approvedAlternative("VibeCheck AI").category("PRODUCT_NAME").rationale("Official brand spelling and casing.").active(true).build()
            );
            termRepository.saveAll(defaultTerms);
            terms = termRepository.findByActiveTrue();
        }
        return ResponseEntity.ok(terms);
    }

    @PostMapping("/terms")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<BrandTerm> createTerm(
            @RequestBody BrandTerm term,
            @AuthenticationPrincipal UserDetails userDetails) {
        BrandTerm saved = termRepository.save(term);
        auditService.recordAsync(null, userDetails.getUsername(), "TERM_CREATED",
                "BrandTerm", saved.getId(), "Term: " + saved.getTerm(), null);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/terms/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<?> deactivateTerm(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return termRepository.findById(id).map(t -> {
            t.setActive(false);
            termRepository.save(t);
            auditService.recordAsync(null, userDetails.getUsername(), "TERM_DEACTIVATED",
                    "BrandTerm", id, "Term: " + t.getTerm(), null);
            return ResponseEntity.ok("Term deactivated");
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Facts & Claims Repository: list active facts.
     */
    @GetMapping("/facts")
    public ResponseEntity<List<BrandFactClaim>> getFacts() {
        List<BrandFactClaim> facts = factClaimRepository.findByActiveTrue();
        if (facts.isEmpty()) {
            List<BrandFactClaim> defaultFacts = List.of(
                    BrandFactClaim.builder().topic("Security").verifiedClaim("ISO 27001 compliant infrastructure with AES-256 data encryption at rest.").sourceReference("Security Whitepaper 2026").category("SECURITY").active(true).build(),
                    BrandFactClaim.builder().topic("Performance").verifiedClaim("Sub-second deterministic rule matching and hybrid AI verification engine.").sourceReference("Platform Architecture v2").category("PRODUCT").active(true).build()
            );
            factClaimRepository.saveAll(defaultFacts);
            facts = factClaimRepository.findByActiveTrue();
        }
        return ResponseEntity.ok(facts);
    }

    @PostMapping("/facts")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<BrandFactClaim> createFact(
            @RequestBody BrandFactClaim fact,
            @AuthenticationPrincipal UserDetails userDetails) {
        BrandFactClaim saved = factClaimRepository.save(fact);
        auditService.recordAsync(null, userDetails.getUsername(), "FACT_CREATED",
                "BrandFactClaim", saved.getId(), "Topic: " + saved.getTopic(), null);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/facts/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<?> deactivateFact(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return factClaimRepository.findById(id).map(f -> {
            f.setActive(false);
            factClaimRepository.save(f);
            auditService.recordAsync(null, userDetails.getUsername(), "FACT_DEACTIVATED",
                    "BrandFactClaim", id, "Topic: " + f.getTopic(), null);
            return ResponseEntity.ok("Fact deactivated");
        }).orElse(ResponseEntity.notFound().build());
    }
}
