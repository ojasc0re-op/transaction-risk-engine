package com.frauddetection.risk.rule;

import com.frauddetection.config.RiskConfig;
import com.frauddetection.entity.Transaction;
import com.frauddetection.risk.RiskContext;
import com.frauddetection.risk.RiskRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NewDeviceRuleTest {

    private NewDeviceRule rule;
    private RiskConfig riskConfig;

    @BeforeEach
    void setUp() {
        riskConfig = new RiskConfig();
        riskConfig.getScoring().setNewDeviceScore(15);
        rule = new NewDeviceRule(riskConfig);
    }

    @Test
    @DisplayName("Known device does not trigger rule")
    void testKnownDeviceDoesNotTrigger() {
        Transaction txn = Transaction.builder().deviceId("device-known-001").build();
        RiskContext context = RiskContext.builder()
                .transaction(txn)
                .newDevice(false)
                .build();

        RiskRule.RuleEvaluation eval = rule.evaluate(context);
        assertThat(eval.triggered()).isFalse();
        assertThat(eval.scoreAdded()).isEqualTo(0);
    }

    @Test
    @DisplayName("New device triggers rule with +15 points")
    void testNewDeviceTriggers() {
        Transaction txn = Transaction.builder().deviceId("device-unseen-xyz").build();
        RiskContext context = RiskContext.builder()
                .transaction(txn)
                .newDevice(true)
                .build();

        RiskRule.RuleEvaluation eval = rule.evaluate(context);
        assertThat(eval.triggered()).isTrue();
        assertThat(eval.scoreAdded()).isEqualTo(15);
        assertThat(eval.reason()).contains("previously unseen device");
    }

    @Test
    @DisplayName("Blank or null device ID does not trigger rule")
    void testBlankDeviceDoesNotTrigger() {
        Transaction txn = Transaction.builder().deviceId("").build();
        RiskContext context = RiskContext.builder()
                .transaction(txn)
                .newDevice(true)
                .build();

        RiskRule.RuleEvaluation eval = rule.evaluate(context);
        assertThat(eval.triggered()).isFalse();
    }
}
