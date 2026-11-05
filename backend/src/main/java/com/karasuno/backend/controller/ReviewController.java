package com.karasuno.backend.controller;

import com.karasuno.backend.dto.ReviewDecisionRequest;
import com.karasuno.backend.entity.DraftReviewLog;
import com.karasuno.backend.repository.DraftReviewLogRepository;
import com.karasuno.backend.repository.UserRepository;
import com.karasuno.backend.service.audit.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST controller for the Human Review Dashboard.
 * Manager-only endpoints for reviewing drafts and submitting approval decisions.
 */
@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final DraftReviewLogRepository draftReviewLogRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ReviewController(DraftReviewLogRepository draftReviewLogRepository,
                            UserRepository userRepository,
                            AuditService auditService) {
        this.draftReviewLogRepository = draftReviewLogRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /** All pending drafts awaiting manager review, paginated, oldest first. */
    @GetMapping("/pending")
    public ResponseEntity<Page<DraftReviewLog>> getPendingReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(draftReviewLogRepository
                .findByReviewStatusOrderBySubmittedAtAsc(
                        DraftReviewLog.ReviewStatus.PENDING_REVIEW,
                        PageRequest.of(page, size)));
    }

    /** All drafts (all statuses), paginated, newest first. */
    @GetMapping("/all")
    public ResponseEntity<Page<DraftReviewLog>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(draftReviewLogRepository
                .findAllByOrderBySubmittedAtDesc(PageRequest.of(page, size)));
    }

    /** Retrieves a single draft review log with full details. */
    @GetMapping("/{id}")
    public ResponseEntity<DraftReviewLog> getReview(@PathVariable Long id) {
        return draftReviewLogRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Submits a manager's review decision for a draft.
     * Allowed decisions: APPROVED, REJECTED, NEEDS_REVISION.
     */
    @PostMapping("/{id}/decision")
    public ResponseEntity<?> submitDecision(
            @PathVariable Long id,
            @RequestBody ReviewDecisionRequest decisionRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        return draftReviewLogRepository.findById(id).map(draftLog -> {
            Long managerId = userRepository.findByUsername(userDetails.getUsername())
                    .map(u -> u.getId()).orElse(0L);

            draftLog.setReviewStatus(decisionRequest.getDecision());
            draftLog.setReviewedByManagerId(managerId);
            draftLog.setManagerComments(decisionRequest.getComments());
            draftLog.setReviewedAt(LocalDateTime.now());
            DraftReviewLog updated = draftReviewLogRepository.save(draftLog);

            auditService.recordAsync(managerId, userDetails.getUsername(),
                    "MANAGER_" + decisionRequest.getDecision().name(),
                    "DraftReviewLog", id,
                    "Decision: " + decisionRequest.getDecision() +
                    " | Comments: " + decisionRequest.getComments(), null);

            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    /** Dashboard statistics: draft counts by review status. */
    @GetMapping("/stats")
    public ResponseEntity<ReviewStats> getStats() {
        return ResponseEntity.ok(new ReviewStats(
                draftReviewLogRepository.countByReviewStatus(DraftReviewLog.ReviewStatus.PENDING_REVIEW),
                draftReviewLogRepository.countByReviewStatus(DraftReviewLog.ReviewStatus.APPROVED),
                draftReviewLogRepository.countByReviewStatus(DraftReviewLog.ReviewStatus.REJECTED),
                draftReviewLogRepository.countByReviewStatus(DraftReviewLog.ReviewStatus.NEEDS_REVISION)
        ));
    }

    /** Statistics record for the manager dashboard widget. */
    public record ReviewStats(long pending, long approved, long rejected, long needsRevision) {}
}
