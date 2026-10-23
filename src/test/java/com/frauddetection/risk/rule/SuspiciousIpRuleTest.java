package com.frauddetection.risk.rule;

import com.frauddetection.config.RiskConfig;
import com.frauddetection.entity.Transaction;
import com.frauddetection.risk.RiskContext;
import com.frauddetection.risk.RiskRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SuspiciousIpRuleTest {

    private SuspiciousIpRule rule;
    private RiskConfig riskConfig;

    @BeforeEach
    void setUp() {
        riskConfig = new RiskConfig();
        riskConfig.setSuspiciousIps(List.of("192.168.100.1", "10.0.0.1"));
        riskConfig.getScoring().setSuspiciousIpScore(20);
        rule = new SuspiciousIpRule(riskConfig);
    }

    @Test
    @DisplayName("Normal IP not on suspicious list does not trigger rule")
    void testNormalIpDoesNotTrigger() {
        Transaction txn = Transaction.builder().ipAddress("192.168.1.50").build();
        RiskContext context = RiskContext.builder().transaction(txn).build();

        RiskRule.RuleEvaluation eval = rule.evaluate(context);
        assertThat(eval.triggered()).isFalse();
        assertThat(eval.scoreAdded()).isEqualTo(0);
    }

    @Test
    @DisplayName("Flagged suspicious IP triggers rule with +20 points")
    void testSuspiciousIpTriggers() {
        Transaction txn = Transaction.builder().ipAddress("192.168.100.1").build();
        RiskContext context = RiskContext.builder().transaction(txn).build();

        RiskRule.RuleEvaluation eval = rule.evaluate(context);
        assertThat(eval.triggered()).isTrue();
        assertThat(eval.scoreAdded()).isEqualTo(20);
        assertThat(eval.reason()).contains("flagged suspicious IP");
    }

    @Test
    @DisplayName("Null or empty IP does not trigger rule")
    void testNullIpDoesNotTrigger() {
        Transaction txn = Transaction.builder().ipAddress(null).build();
        RiskContext context = RiskContext.builder().transaction(txn).build();

        RiskRule.RuleEvaluation eval = rule.evaluate(context);
        assertThat(eval.triggered()).isFalse();
    }
}
