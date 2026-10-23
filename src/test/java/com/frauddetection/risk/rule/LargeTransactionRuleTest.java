package com.frauddetection.risk.rule;

import com.frauddetection.config.RiskConfig;
import com.frauddetection.entity.Transaction;
import com.frauddetection.risk.RiskContext;
import com.frauddetection.risk.RiskRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LargeTransactionRuleTest {

    private LargeTransactionRule rule;
    private RiskConfig riskConfig;

    @BeforeEach
    void setUp() {
        riskConfig = new RiskConfig();
        riskConfig.getThresholds().setLargeTransactionMultiplier(3.0);
        riskConfig.getScoring().setLargeTransactionScore(20);
        rule = new LargeTransactionRule(riskConfig);
    }

    @Test
    @DisplayName("Transaction within normal bounds (under 3x average) does not trigger rule")
    void testNormalAmountDoesNotTrigger() {
        Transaction txn = Transaction.builder().amount(new BigDecimal("250.00")).build();
        RiskContext context = RiskContext.builder()
                .transaction(txn)
                .userAvgAmount(new BigDecimal("100.00"))
                .build();

        RiskRule.RuleEvaluation eval = rule.evaluate(context);
        assertThat(eval.triggered()).isFalse();
        assertThat(eval.scoreAdded()).isEqualTo(0);
    }

    @Test
    @DisplayName("Transaction exceeding 3x average triggers rule and adds 20 points")
    void testLargeAmountTriggers() {
        Transaction txn = Transaction.builder().amount(new BigDecimal("350.00")).build();
        RiskContext context = RiskContext.builder()
                .transaction(txn)
                .userAvgAmount(new BigDecimal("100.00"))
                .build();

        RiskRule.RuleEvaluation eval = rule.evaluate(context);
        assertThat(eval.triggered()).isTrue();
        assertThat(eval.scoreAdded()).isEqualTo(20);
        assertThat(eval.reason()).contains("Unusual transaction amount");
    }

    @Test
    @DisplayName("New user with zero average amount does not trigger rule")
    void testZeroAverageDoesNotTrigger() {
        Transaction txn = Transaction.builder().amount(new BigDecimal("5000.00")).build();
        RiskContext context = RiskContext.builder()
                .transaction(txn)
                .userAvgAmount(BigDecimal.ZERO)
                .build();

        RiskRule.RuleEvaluation eval = rule.evaluate(context);
        assertThat(eval.triggered()).isFalse();
    }
}
