package com.frauddetection.risk.rule;

import com.frauddetection.config.RiskConfig;
import com.frauddetection.risk.RiskContext;
import com.frauddetection.risk.RiskRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Rule 5: Suspicious IP Address
 * Flags transactions from addresses matching the configurable suspicious IP list.
 */
@Component
@RequiredArgsConstructor
public class SuspiciousIpRule implements RiskRule {

    private final RiskConfig riskConfig;

    @Override
    public RuleEvaluation evaluate(RiskContext context) {
        String ip = context.getTransaction().getIpAddress();
        if (ip == null || ip.isBlank()) {
            return RuleEvaluation.notTriggered();
        }

        if (riskConfig.getSuspiciousIps() != null && riskConfig.getSuspiciousIps().contains(ip.trim())) {
            return RuleEvaluation.triggered(
                    riskConfig.getScoring().getSuspiciousIpScore(),
                    "Transaction from a flagged suspicious IP: " + ip
            );
        }
        return RuleEvaluation.notTriggered();
    }

    @Override
    public String getName() {
        return "SuspiciousIpRule";
    }
}
