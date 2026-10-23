package com.frauddetection.controller;

import com.frauddetection.dto.response.RiskAssessmentResponse;
import com.frauddetection.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
@Tag(name = "Risk Assessment", description = "Risk scoring details and triggered fraud rules")
@SecurityRequirement(name = "bearerAuth")
public class RiskController {

    private final TransactionService transactionService;

    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Get risk assessment breakdown for a transaction",
               description = "Returns the risk score, risk level, decision, and reasons for triggered rules.")
    public ResponseEntity<RiskAssessmentResponse> getRiskAssessment(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(transactionService.getRiskAssessment(transactionId));
    }
}
