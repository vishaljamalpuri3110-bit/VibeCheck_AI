package com.karasuno.backend.controller;

import com.karasuno.backend.entity.AuditLog;
import com.karasuno.backend.service.audit.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for audit log retrieval.
 * All endpoints restricted to ROLE_ADMIN via SecurityConfig URL rules.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /** All audit logs, newest first, paginated. */
    @GetMapping
    public ResponseEntity<Page<AuditLog>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getAllLogs(PageRequest.of(page, size)));
    }

    /** Audit logs for a specific user. */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AuditLog>> getLogsForUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getLogsForUser(userId, PageRequest.of(page, size)));
    }

    /** Audit trail for a specific entity instance. */
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<Page<AuditLog>> getLogsForEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getLogsForEntity(
                entityType, entityId, PageRequest.of(page, size)));
    }
}
