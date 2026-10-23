package com.frauddetection.risk.engine;

import com.frauddetection.config.RiskConfig;
import com.frauddetection.entity.Transaction;
import com.frauddetection.entity.User;
import com.frauddetection.repository.DeviceRepository;
import com.frauddetection.repository.TransactionRepository;
import com.frauddetection.risk.RiskContext;
import com.frauddetection.risk.RiskResult;
import com.frauddetection.risk.RiskRule;
import com.frauddetection.service.RedisTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Core Rule-Based Risk Engine.
 *
 * Workflow:
 * 1. Assemble RiskContext gathering device status, Redis counts, and past transactions.
 * 2. Execute all registered RiskRule components.
 * 3. Aggregate and normalize score to 0–100 scale.
 * 4. Determine RiskLevel (LOW, MEDIUM, HIGH, CRITICAL).
 * 5. Determine automated decision (APPROVED, REVIEW, BLOCKED).
 * 6. Update Redis counters.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskEngine {

    private final List<RiskRule> rules;
    private final RiskConfig riskConfig;
    private final TransactionRepository transactionRepository;
    private final DeviceRepository deviceRepository;
    private final RedisTransactionService redisTransactionService;

    public RiskResult evaluate(Transaction transaction, User user) {
        log.debug("Evaluating transaction risk for user={}, amount={}", user.getUsername(), transaction.getAmount());

        RiskContext context = buildContext(transaction, user);

        List<String> triggeredReasons = new ArrayList<>();
        int rawScore = 0;

        for (RiskRule rule : rules) {
            RiskRule.RuleEvaluation evaluation = rule.evaluate(context);
            if (evaluation.triggered()) {
                rawScore += evaluation.scoreAdded();
                triggeredReasons.add(evaluation.reason());
                log.debug("Rule [{}] triggered: +{} pts ({})",
                        rule.getName(), evaluation.scoreAdded(), evaluation.reason());
            }
        }

        // Normalize between 0 and 100
        int normalizedScore = Math.min(Math.max(rawScore, 0), 100);

        Transaction.RiskLevel level = classifyRiskLevel(normalizedScore);
        Transaction.TransactionStatus decision = determineDecision(level);

        // Update Redis sliding window counters
        redisTransactionService.incrementTransactionCount(
                user.getId(),
                riskConfig.getThresholds().getFrequencyWindowMinutes()
        );

        if (decision == Transaction.TransactionStatus.BLOCKED) {
            redisTransactionService.incrementFailedTransactionCount(
                    user.getId(),
                    riskConfig.getThresholds().getFailedTransactionWindowMinutes()
            );
        }

        log.info("Risk evaluation complete: user={}, score={}, level={}, decision={}, rulesTriggered={}",
                user.getUsername(), normalizedScore, level, decision, triggeredReasons.size());

        return new RiskResult(normalizedScore, level, decision, triggeredReasons);
    }

    private RiskContext buildContext(Transaction transaction, User user) {
        int frequencyWindow = riskConfig.getThresholds().getFrequencyWindowMinutes();
        Instant windowStart = Instant.now().minus(frequencyWindow, ChronoUnit.MINUTES);

        List<Transaction> recentTransactions = transactionRepository
                .findRecentByUserId(user.getId(), windowStart);

        long txnCount = redisTransactionService.getTransactionCount(user.getId(), frequencyWindow);
        long failedCount = redisTransactionService.getFailedTransactionCount(user.getId());

        boolean newDevice = transaction.getDeviceId() != null
                && !transaction.getDeviceId().isBlank()
                && !deviceRepository.existsByUserIdAndDeviceFingerprint(user.getId(), transaction.getDeviceId());

        BigDecimal avgAmount = user.getAvgTransactionAmount();
        if (avgAmount == null || avgAmount.compareTo(BigDecimal.ZERO) == 0) {
            avgAmount = transactionRepository.findAverageAmountByUserId(user.getId())
                    .orElse(BigDecimal.ZERO);
        }

        return RiskContext.builder()
                .transaction(transaction)
                .user(user)
                .recentTransactions(recentTransactions)
                .transactionCountInWindow(txnCount)
                .newDevice(newDevice)
                .userAvgAmount(avgAmount)
                .failedTransactionCount(failedCount)
                .build();
    }

    public Transaction.RiskLevel classifyRiskLevel(int score) {
        RiskConfig.Levels levels = riskConfig.getLevels();
        if (score <= levels.getLowMax()) return Transaction.RiskLevel.LOW;
        if (score <= levels.getMediumMax()) return Transaction.RiskLevel.MEDIUM;
        if (score <= levels.getHighMax()) return Transaction.RiskLevel.HIGH;
        return Transaction.RiskLevel.CRITICAL;
    }

    public Transaction.TransactionStatus determineDecision(Transaction.RiskLevel level) {
        return switch (level) {
            case LOW, MEDIUM -> Transaction.TransactionStatus.APPROVED;
            case HIGH -> Transaction.TransactionStatus.REVIEW;
            case CRITICAL -> Transaction.TransactionStatus.BLOCKED;
        };
    }
}
