package com.frauddetection.controller;

import com.frauddetection.dto.response.DashboardStatsResponse;
import com.frauddetection.dto.response.PagedResponse;
import com.frauddetection.dto.response.TransactionResponse;
import com.frauddetection.service.DashboardService;
import com.frauddetection.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrator and analyst review endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final TransactionService transactionService;
    private final DashboardService dashboardService;

    @GetMapping("/transactions/high-risk")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Get high-risk and critical transactions",
               description = "Returns paginated list of transactions classified as HIGH or CRITICAL.")
    public ResponseEntity<PagedResponse<TransactionResponse>> getHighRiskTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(transactionService.getHighRiskTransactions(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/risk-statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Get system-wide risk statistics",
               description = "Returns aggregate counts, averages, and risk level distributions.")
    public ResponseEntity<DashboardStatsResponse> getRiskStatistics() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/transactions/suspicious")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Get recent suspicious transactions",
               description = "Returns top recent suspicious transactions for rapid triage.")
    public ResponseEntity<List<TransactionResponse>> getRecentSuspicious(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(transactionService.getRecentSuspicious(Math.min(limit, 50)));
    }
}
