package com.frauddetection.dto.response;

import com.frauddetection.entity.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID userId,
        String username,
        BigDecimal amount,
        String currency,
        String merchant,
        Transaction.TransactionType transactionType,
        Instant transactionTimestamp,
        String location,
        String deviceId,
        Transaction.TransactionStatus status,
        int riskScore,
        Transaction.RiskLevel riskLevel,
        Instant createdAt
) {}
