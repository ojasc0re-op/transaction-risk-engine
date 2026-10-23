package com.frauddetection.risk.rule;

import com.frauddetection.config.RiskConfig;
import com.frauddetection.entity.Transaction;
import com.frauddetection.risk.RiskContext;
import com.frauddetection.risk.RiskRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionFrequencyRuleTest {

    private TransactionFrequencyRule rule;
    private RiskConfig riskConfig;

    @BeforeEach
    void setUp() {
        riskConfig = new RiskConfig();
        riskConfig.getThresholds().setFrequencyMaxCount(10);
        riskConfig.getScoring().setFrequencyAnomalyScore(20);
        rule = new TransactionFrequencyRule(riskConfig);
    }

    @Test
    @DisplayName("Transaction count below threshold does not trigger rule")
    void testNormalFrequencyDoesNotTrigger() {
        RiskContext context = RiskContext.builder()
                .transaction(Transaction.builder().build())
                .transactionCountInWindow(5)
                .build();

        RiskRule.RuleEvaluation eval = rule.evaluate(context);
        assertThat(eval.triggered()).isFalse();
        assertThat(eval.scoreAdded()).isEqualTo(0);
    }

    @Test
    @DisplayName("Transaction count at or above threshold triggers rule with +20 points")
    void testBurstFrequencyTriggers() {
        RiskContext context = RiskContext.builder()
                .transaction(Transaction.builder().build())
                .transactionCountInWindow(10)
                .build();

        RiskRule.RuleEvaluation eval = rule.evaluate(context);
        assertThat(eval.triggered()).isTrue();
        assertThat(eval.scoreAdded()).isEqualTo(20);
        assertThat(eval.reason()).contains("Unusual transaction frequency");
    }
}
