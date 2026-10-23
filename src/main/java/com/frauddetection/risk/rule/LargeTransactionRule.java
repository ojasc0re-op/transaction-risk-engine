package com.frauddetection.risk.rule;

import com.frauddetection.config.RiskConfig;
import com.frauddetection.risk.RiskContext;
import com.frauddetection.risk.RiskRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Rule 1: Large Transaction Anomaly
 * Increases risk when transaction amount exceeds the user's historical average
 * by a configurable multiplier (default 3.0x).
 */
@Component
@RequiredArgsConstructor
public class LargeTransactionRule implements RiskRule {

    private final RiskConfig riskConfig;

    @Override
    public RuleEvaluation evaluate(RiskContext context) {
        BigDecimal avg = context.getUserAvgAmount();
        if (avg == null || avg.compareTo(BigDecimal.ZERO) <= 0) {
            return RuleEvaluation.notTriggered();
        }

        double multiplier = riskConfig.getThresholds().getLargeTransactionMultiplier();
        BigDecimal threshold = avg.multiply(BigDecimal.valueOf(multiplier));

        if (context.getTransaction().getAmount().compareTo(threshold) > 0) {
            return RuleEvaluation.triggered(
                    riskConfig.getScoring().getLargeTransactionScore(),
                    String.format("Unusual transaction amount (%.2f exceeds %.1fx average of %.2f)",
                            context.getTransaction().getAmount(),
                            multiplier,
                            avg)
            );
        }
        return RuleEvaluation.notTriggered();
    }

    @Override
    public String getName() {
        return "LargeTransactionRule";
    }
}
