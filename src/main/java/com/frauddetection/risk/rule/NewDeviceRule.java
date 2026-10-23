package com.frauddetection.risk.rule;

import com.frauddetection.config.RiskConfig;
import com.frauddetection.risk.RiskContext;
import com.frauddetection.risk.RiskRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Rule 4: New Device Detection
 * Flags transactions originating from devices not previously seen for the user account.
 */
@Component
@RequiredArgsConstructor
public class NewDeviceRule implements RiskRule {

    private final RiskConfig riskConfig;

    @Override
    public RuleEvaluation evaluate(RiskContext context) {
        String deviceId = context.getTransaction().getDeviceId();
        if (deviceId == null || deviceId.isBlank()) {
            return RuleEvaluation.notTriggered();
        }

        if (context.isNewDevice()) {
            return RuleEvaluation.triggered(
                    riskConfig.getScoring().getNewDeviceScore(),
                    "Transaction from a previously unseen device: " + deviceId
            );
        }
        return RuleEvaluation.notTriggered();
    }

    @Override
    public String getName() {
        return "NewDeviceRule";
    }
}
