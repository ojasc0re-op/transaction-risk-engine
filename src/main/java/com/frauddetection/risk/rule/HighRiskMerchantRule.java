package com.frauddetection.risk.rule;

import com.frauddetection.config.RiskConfig;
import com.frauddetection.risk.RiskContext;
import com.frauddetection.risk.RiskRule;
import com.frauddetection.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Rule 6: Merchant Risk Level
 * Increases risk when the merchant has been identified as HIGH risk in the merchant registry.
 */
@Component
@RequiredArgsConstructor
public class HighRiskMerchantRule implements RiskRule {

    private final RiskConfig riskConfig;
    private final MerchantService merchantService;

    @Override
    public RuleEvaluation evaluate(RiskContext context) {
        String merchantName = context.getTransaction().getMerchant();
        if (merchantName == null || merchantName.isBlank()) {
            return RuleEvaluation.notTriggered();
        }

        if (merchantService.isHighRiskMerchant(merchantName)) {
            return RuleEvaluation.triggered(
                    riskConfig.getScoring().getHighRiskMerchantScore(),
                    "Transaction at a high-risk merchant: " + merchantName
            );
        }
        return RuleEvaluation.notTriggered();
    }

    @Override
    public String getName() {
        return "HighRiskMerchantRule";
    }
}
