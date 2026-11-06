package com.karasuno.backend.controller;

import com.karasuno.backend.dto.CommentRequest;
import com.karasuno.backend.dto.ReviewDecisionRequest;
import com.karasuno.backend.dto.SaveDraftRequest;
import com.karasuno.backend.dto.SubmissionDto;
import com.karasuno.backend.entity.ContentSubmission;
import com.karasuno.backend.entity.DraftReviewLog;
import com.karasuno.backend.entity.SubmissionComment;
import com.karasuno.backend.entity.User;
import com.karasuno.backend.repository.ContentSubmissionRepository;
import com.karasuno.backend.repository.SubmissionCommentRepository;
import com.karasuno.backend.repository.UserRepository;
import com.karasuno.backend.service.audit.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final ContentSubmissionRepository submissionRepository;
    private final SubmissionCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public SubmissionController(ContentSubmissionRepository submissionRepository,
                                SubmissionCommentRepository commentRepository,
                                UserRepository userRepository,
                                AuditService auditService) {
        this.submissionRepository = submissionRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /**
     * Creator saves or updates a draft. Persists draft across sessions, logouts, and page refreshes.
     */
    @PostMapping("/draft")
    public ResponseEntity<SubmissionDto> saveDraft(
            @RequestBody SaveDraftRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();

        ContentSubmission submission;
        if (request.getId() != null) {
            submission = submissionRepository.findById(request.getId())
                    .orElse(new ContentSubmission());
            // Security check: verify ownership if not manager/admin
            if (submission.getId() != null && !submission.getCreatorUserId().equals(user.getId())
                    && !user.getRoles().contains("ROLE_MANAGER") && !user.getRoles().contains("ROLE_ADMIN")) {
                return ResponseEntity.status(403).build();
            }
        } else {
            submission = new ContentSubmission();
            submission.setCreatorUserId(user.getId());
            submission.setCreatorUsername(user.getUsername());
            submission.setStatus(ContentSubmission.SubmissionStatus.DRAFT);
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            submission.setTitle(request.getTitle().trim());
        }
        if (request.getContentType() != null) {
            submission.setContentType(request.getContentType());
        }
        submission.setContent(request.getContent() != null ? request.getContent() : "");
        submission.setLatestDraftReviewLogId(request.getLatestDraftReviewLogId());

        if (request.getOverallState() != null) {
            try {
                submission.setOverallState(DraftReviewLog.VerificationState.valueOf(request.getOverallState()));
            } catch (Exception ignored) {}
        }
        if (request.getVoiceScore() != null) {
            submission.setVoiceScore(request.getVoiceScore());
        }
        if (request.getVoiceFeedback() != null) {
            submission.setVoiceFeedback(request.getVoiceFeedback());
        }
        if (request.getScanResponseSnapshot() != null) {
            submission.setScanResponseSnapshot(request.getScanResponseSnapshot());
        }
        if (request.getCreatorNote() != null) {
            submission.setCreatorNote(request.getCreatorNote());
        }

        ContentSubmission saved = submissionRepository.save(submission);
        auditService.recordAsync(user.getId(), user.getUsername(), "DRAFT_SAVED",
                "ContentSubmission", saved.getId(), "Title: " + saved.getTitle(), null);

        return ResponseEntity.ok(toDto(saved));
    }

    /**
     * Creator submits draft for review by Manager / Admin.
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<SubmissionDto> submitForReview(
            @PathVariable Long id,
            @RequestBody(required = false) SaveDraftRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return submissionRepository.findById(id).map(sub -> {
            if (!sub.getCreatorUserId().equals(user.getId())
                    && !user.getRoles().contains("ROLE_MANAGER") && !user.getRoles().contains("ROLE_ADMIN")) {
                return ResponseEntity.status(403).<SubmissionDto>build();
            }

            sub.setStatus(ContentSubmission.SubmissionStatus.IN_REVIEW);
            sub.setSubmittedAt(LocalDateTime.now());
            if (request != null && request.getCreatorNote() != null) {
                sub.setCreatorNote(request.getCreatorNote());
            }

            ContentSubmission saved = submissionRepository.save(sub);

            // If creator attached a note, add it as the first discussion comment
            if (request != null && request.getCreatorNote() != null && !request.getCreatorNote().isBlank()) {
                SubmissionComment comment = SubmissionComment.builder()
                        .submissionId(saved.getId())
                        .authorUserId(user.getId())
                        .authorUsername(user.getUsername())
                        .authorRole(user.getRoles())
                        .message("Creator Note: " + request.getCreatorNote())
                        .build();
                commentRepository.save(comment);
            }

            auditService.recordAsync(user.getId(), user.getUsername(), "DRAFT_SUBMITTED",
                    "ContentSubmission", saved.getId(), "Submitted for manager review", null);

            return ResponseEntity.ok(toDto(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all drafts and submissions belonging to the logged-in creator.
     */
    @GetMapping("/my-drafts")
    public ResponseEntity<List<SubmissionDto>> getMyDrafts(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        List<ContentSubmission> list = submissionRepository.findByCreatorUserIdOrderByUpdatedAtDesc(user.getId());
        return ResponseEntity.ok(list.stream().map(this::toDto).toList());
    }

    /**
     * Retrieves a single submission with comments. Accessible to Creator (own), Manager, and Admin.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SubmissionDto> getSubmission(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return submissionRepository.findById(id).map(sub -> {
            if (!sub.getCreatorUserId().equals(user.getId())
                    && !user.getRoles().contains("ROLE_MANAGER") && !user.getRoles().contains("ROLE_ADMIN")) {
                return ResponseEntity.status(403).<SubmissionDto>build();
            }
            return ResponseEntity.ok(toDto(sub));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves pending submissions awaiting review (Manager & Admin).
     */
    @GetMapping("/review/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Page<SubmissionDto>> getPendingReviewQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ContentSubmission> pageResult = submissionRepository.findByStatusOrderBySubmittedAtDesc(
                ContentSubmission.SubmissionStatus.IN_REVIEW, PageRequest.of(page, size));
        return ResponseEntity.ok(pageResult.map(this::toDto));
    }

    /**
     * Retrieves all submissions across all statuses (Manager & Admin).
     */
    @GetMapping("/review/all")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Page<SubmissionDto>> getAllSubmissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ContentSubmission> pageResult = submissionRepository.findAllByOrderByUpdatedAtDesc(
                PageRequest.of(page, size));
        return ResponseEntity.ok(pageResult.map(this::toDto));
    }

    /**
     * Review decision (Approve, Reject, Request Changes) by Manager or Admin.
     */
    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<SubmissionDto> reviewDecision(
            @PathVariable Long id,
            @RequestBody ReviewDecisionRequest decisionRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        User reviewer = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return submissionRepository.findById(id).map(sub -> {
            ContentSubmission.SubmissionStatus newStatus;
            switch (decisionRequest.getDecision()) {
                case APPROVED -> newStatus = ContentSubmission.SubmissionStatus.APPROVED;
                case REJECTED -> newStatus = ContentSubmission.SubmissionStatus.REJECTED;
                default -> newStatus = ContentSubmission.SubmissionStatus.NEEDS_REVISION;
            }

            sub.setStatus(newStatus);
            sub.setReviewedByUserId(reviewer.getId());
            sub.setReviewedByUsername(reviewer.getUsername());
            sub.setManagerComments(decisionRequest.getComments());
            sub.setReviewedAt(LocalDateTime.now());

            ContentSubmission saved = submissionRepository.save(sub);

            // Add manager comments to discussion thread
            if (decisionRequest.getComments() != null && !decisionRequest.getComments().isBlank()) {
                SubmissionComment comment = SubmissionComment.builder()
                        .submissionId(saved.getId())
                        .authorUserId(reviewer.getId())
                        .authorUsername(reviewer.getUsername())
                        .authorRole(reviewer.getRoles())
                        .message("[" + newStatus + "] " + decisionRequest.getComments())
                        .build();
                commentRepository.save(comment);
            }

            auditService.recordAsync(reviewer.getId(), reviewer.getUsername(),
                    "REVIEW_" + newStatus.name(), "ContentSubmission", saved.getId(),
                    "Decision: " + newStatus + " | Comments: " + decisionRequest.getComments(), null);

            return ResponseEntity.ok(toDto(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Adds a threaded comment / message on a submission.
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<SubmissionComment> addComment(
            @PathVariable Long id,
            @RequestBody CommentRequest commentRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return submissionRepository.findById(id).map(sub -> {
            if (!sub.getCreatorUserId().equals(user.getId())
                    && !user.getRoles().contains("ROLE_MANAGER") && !user.getRoles().contains("ROLE_ADMIN")) {
                return ResponseEntity.status(403).<SubmissionComment>build();
            }

            if (commentRequest.getMessage() == null || commentRequest.getMessage().isBlank()) {
                return ResponseEntity.badRequest().<SubmissionComment>build();
            }

            SubmissionComment comment = SubmissionComment.builder()
                    .submissionId(sub.getId())
                    .authorUserId(user.getId())
                    .authorUsername(user.getUsername())
                    .authorRole(user.getRoles())
                    .message(commentRequest.getMessage().trim())
                    .build();

            SubmissionComment saved = commentRepository.save(comment);
            auditService.recordAsync(user.getId(), user.getUsername(), "COMMENT_ADDED",
                    "ContentSubmission", sub.getId(), "Comment: " + saved.getMessage(), null);

            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lists all threaded comments for a submission.
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<SubmissionComment>> getComments(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return submissionRepository.findById(id).map(sub -> {
            if (!sub.getCreatorUserId().equals(user.getId())
                    && !user.getRoles().contains("ROLE_MANAGER") && !user.getRoles().contains("ROLE_ADMIN")) {
                return ResponseEntity.status(403).<List<SubmissionComment>>build();
            }
            return ResponseEntity.ok(commentRepository.findBySubmissionIdOrderByCreatedAtAsc(id));
        }).orElse(ResponseEntity.notFound().build());
    }

    private SubmissionDto toDto(ContentSubmission s) {
        SubmissionDto dto = new SubmissionDto();
        dto.setId(s.getId());
        dto.setTitle(s.getTitle());
        dto.setContentType(s.getContentType());
        dto.setCreatorUserId(s.getCreatorUserId());
        dto.setCreatorUsername(s.getCreatorUsername());
        dto.setContent(s.getContent());
        dto.setStatus(s.getStatus());
        dto.setLatestDraftReviewLogId(s.getLatestDraftReviewLogId());
        dto.setOverallState(s.getOverallState() != null ? s.getOverallState().name() : null);
        dto.setVoiceScore(s.getVoiceScore());
        dto.setVoiceFeedback(s.getVoiceFeedback());
        dto.setCreatorNote(s.getCreatorNote());
        dto.setReviewedByUserId(s.getReviewedByUserId());
        dto.setReviewedByUsername(s.getReviewedByUsername());
        dto.setManagerComments(s.getManagerComments());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setUpdatedAt(s.getUpdatedAt());
        dto.setSubmittedAt(s.getSubmittedAt());
        dto.setReviewedAt(s.getReviewedAt());
        dto.setCommentCount((int) commentRepository.countBySubmissionId(s.getId()));
        return dto;
    }
}
