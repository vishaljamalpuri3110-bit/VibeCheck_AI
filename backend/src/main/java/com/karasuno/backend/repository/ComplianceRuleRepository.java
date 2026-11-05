package com.karasuno.backend.repository;

import com.karasuno.backend.entity.ComplianceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/** JPA repository for ComplianceRule — loads active rules for local pre-screening. */
@Repository
public interface ComplianceRuleRepository extends JpaRepository<ComplianceRule, Long> {

    /** All currently active rules (called on every compliance scan) */
    List<ComplianceRule> findByActiveTrue();

    /** Active rules of a specific type */
    List<ComplianceRule> findByActiveTrueAndRuleType(ComplianceRule.RuleType ruleType);

    /** Active rules at or above specified severities */
    List<ComplianceRule> findByActiveTrueAndSeverityIn(List<ComplianceRule.Severity> severities);
}
