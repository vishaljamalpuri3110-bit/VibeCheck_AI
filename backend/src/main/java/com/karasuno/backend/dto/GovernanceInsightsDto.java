package com.karasuno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovernanceInsightsDto {
    private long totalScans;
    private long totalSubmissions;
    private long approvedSubmissions;
    private long rejectedSubmissions;
    private long pendingSubmissions;
    private double overallComplianceRate; // e.g. 84.5%
    private List<ViolationCount> topBrokenRules;
    private List<SeverityDistribution> severityBreakdown;
    private List<CreatorCompliance> creatorLeaderboard;
    private Map<String, Long> contentTypeDistribution;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ViolationCount {
        private String ruleName;
        private String severity;
        private long count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SeverityDistribution {
        private String severity;
        private long count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreatorCompliance {
        private String username;
        private long totalSubmissions;
        private long approvedCount;
        private double complianceRate;
    }
}
