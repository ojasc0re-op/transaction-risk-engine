package com.frauddetection.risk.rule;

import com.frauddetection.config.RiskConfig;
import com.frauddetection.risk.RiskContext;
import com.frauddetection.risk.RiskRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Rule 2: Transaction Frequency Anomaly
 * Detects rapid or burst transactions within a configurable time window using Redis counters.
 */
@Component
@RequiredArgsConstructor
public class TransactionFrequencyRule implements RiskRule {

    private final RiskConfig riskConfig;

    @Override
    public RuleEvaluation evaluate(RiskContext context) {
        int maxCount = riskConfig.getThresholds().getFrequencyMaxCount();
        long count = context.getTransactionCountInWindow();

        if (count >= maxCount) {
            return RuleEvaluation.triggered(
                    riskConfig.getScoring().getFrequencyAnomalyScore(),
                    String.format("Unusual transaction frequency: %d transactions in window (threshold: %d)",
                            count, maxCount)
            );
        }
        return RuleEvaluation.notTriggered();
    }

    @Override
    public String getName() {
        return "TransactionFrequencyRule";
    }
}
