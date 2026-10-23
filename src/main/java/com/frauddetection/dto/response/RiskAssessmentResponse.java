package com.frauddetection.dto.response;

import com.frauddetection.entity.Transaction;

import java.util.List;
import java.util.UUID;

public record RiskAssessmentResponse(
        UUID transactionId,
        int riskScore,
        Transaction.RiskLevel riskLevel,
        Transaction.TransactionStatus decision,
        List<String> reasons
) {}
