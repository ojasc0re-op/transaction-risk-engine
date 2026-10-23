package com.frauddetection.dto.response;

import java.util.Map;

public record DashboardStatsResponse(
        long totalTransactions,
        long approvedTransactions,
        long blockedTransactions,
        long reviewTransactions,
        double averageRiskScore,
        long highRiskCount,
        Map<String, Long> riskDistribution
) {}
