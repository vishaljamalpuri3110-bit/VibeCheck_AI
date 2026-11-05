package com.karasuno.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Deterministic compliance rule persisted in the database.
 * Two supported types:
 *  - BANNED_KEYWORD: case-insensitive exact substring match
 *  - REGEX_PATTERN:  Java-compatible regular expression
 */
@Entity
@Table(name = "compliance_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable rule name, e.g. "No Guaranteed Returns Claim" */
    @Column(nullable = false, length = 200)
    private String ruleName;

    /** Rule type: BANNED_KEYWORD or REGEX_PATTERN */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RuleType ruleType;

    /** The keyword string or regex pattern to match */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String pattern;

    /** Severity: LOW, MEDIUM, HIGH, CRITICAL */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    /** Why this rule exists — shown in violation cards */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Whether this rule is currently active */
    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (active == null) active = true;
    }

    public enum RuleType {
        BANNED_KEYWORD,
        REGEX_PATTERN
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
