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

class LocationAnomalyRuleTest {

    private LocationAnomalyRule rule;
    private RiskConfig riskConfig;

    @BeforeEach
    void setUp() {
        riskConfig = new RiskConfig();
        riskConfig.getThresholds().setLocationDistanceKm(500.0);
        riskConfig.getScoring().setLocationAnomalyScore(25);
        rule = new LocationAnomalyRule(riskConfig);
    }

    @Test
    @DisplayName("Transactions close to each other (e.g. within same city) do not trigger rule")
    void testNearbyLocationDoesNotTrigger() {
        // NYC to Jersey City ~5 km
        Transaction recent = Transaction.builder()
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();

        Transaction current = Transaction.builder()
                .latitude(40.7178)
                .longitude(-74.0431)
                .build();

        RiskContext context = RiskContext.builder()
                .transaction(current)
                .recentTransactions(List.of(recent))
                .build();

        RiskRule.RuleEvaluation eval = rule.evaluate(context);
        assertThat(eval.triggered()).isFalse();
    }

    @Test
    @DisplayName("Transaction far away (e.g. NYC to LA ~3900 km) triggers rule with +25 points")
    void testDistantLocationTriggers() {
        // NYC (40.7128, -74.0060) to LA (34.0522, -118.2437)
        Transaction recent = Transaction.builder()
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();

        Transaction current = Transaction.builder()
                .latitude(34.0522)
                .longitude(-118.2437)
                .build();

        RiskContext context = RiskContext.builder()
                .transaction(current)
                .recentTransactions(List.of(recent))
                .build();

        RiskRule.RuleEvaluation eval = rule.evaluate(context);
        assertThat(eval.triggered()).isTrue();
        assertThat(eval.scoreAdded()).isEqualTo(25);
        assertThat(eval.reason()).contains("Suspicious location change");
    }

    @Test
    @DisplayName("Transaction without coordinates is skipped safely")
    void testMissingCoordinatesHandledSafely() {
        Transaction current = Transaction.builder()
                .latitude(null)
                .longitude(null)
                .build();

        RiskContext context = RiskContext.builder()
                .transaction(current)
                .recentTransactions(List.of())
                .build();

        RiskRule.RuleEvaluation eval = rule.evaluate(context);
        assertThat(eval.triggered()).isFalse();
    }
}
