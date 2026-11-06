package com.karasuno.backend.controller;

import com.karasuno.backend.dto.GovernanceInsightsDto;
import com.karasuno.backend.service.governance.GovernanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance")
public class GovernanceController {

    private final GovernanceService governanceService;

    public GovernanceController(GovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @GetMapping("/insights")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<GovernanceInsightsDto> getInsights() {
        return ResponseEntity.ok(governanceService.getInsights());
    }
}
