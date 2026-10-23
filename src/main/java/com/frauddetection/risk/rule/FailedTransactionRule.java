package com.frauddetection.risk.rule;

import com.frauddetection.config.RiskConfig;
import com.frauddetection.risk.RiskContext;
import com.frauddetection.risk.RiskRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Rule 7: Multiple Failed Transactions
 * Detects patterns of consecutive failed or blocked transactions, which often indicate
 * card testing or brute-forcing behavior.
 */
@Component
@RequiredArgsConstructor
public class FailedTransactionRule implements RiskRule {

    private final RiskConfig riskConfig;

    @Override
    public RuleEvaluation evaluate(RiskContext context) {
        int maxFailed = riskConfig.getThresholds().getFailedTransactionMaxCount();
        long failedCount = context.getFailedTransactionCount();

        if (failedCount >= maxFailed) {
            return RuleEvaluation.triggered(
                    riskConfig.getScoring().getFailedTransactionScore(),
                    String.format("Multiple recent failed transactions: %d failures in window", failedCount)
            );
        }
        return RuleEvaluation.notTriggered();
    }

    @Override
    public String getName() {
        return "FailedTransactionRule";
    }
}
