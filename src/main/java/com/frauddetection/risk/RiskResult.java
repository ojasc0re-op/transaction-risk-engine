package com.frauddetection.risk;

import com.frauddetection.entity.Transaction;

import java.util.List;

/**
 * Output of the RiskEngine evaluation.
 *
 * @param score    Normalized risk score (0-100)
 * @param level    Categorical risk level (LOW, MEDIUM, HIGH, CRITICAL)
 * @param decision Automated transaction decision (APPROVED, REVIEW, BLOCKED)
 * @param reasons  Triggered rule explanations
 */
public record RiskResult(
        int score,
        Transaction.RiskLevel level,
        Transaction.TransactionStatus decision,
        List<String> reasons
) {}
