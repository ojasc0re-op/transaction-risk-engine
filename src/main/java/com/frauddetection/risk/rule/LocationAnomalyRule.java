package com.frauddetection.risk.rule;

import com.frauddetection.config.RiskConfig;
import com.frauddetection.entity.Transaction;
import com.frauddetection.risk.RiskContext;
import com.frauddetection.risk.RiskRule;
import com.frauddetection.util.GeoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rule 3: Location Anomaly
 * Detects suspicious location shifts compared to recent transactions using Haversine formula.
 *
 * Simplification Note: Uses a configurable distance threshold (e.g. 500 km) from recent transactions.
 * It is clearly documented as a demo heuristic rather than impossible-travel time-velocity modeling.
 */
@Component
@RequiredArgsConstructor
public class LocationAnomalyRule implements RiskRule {

    private final RiskConfig riskConfig;

    @Override
    public RuleEvaluation evaluate(RiskContext context) {
        Transaction current = context.getTransaction();
        if (current.getLatitude() == null || current.getLongitude() == null) {
            return RuleEvaluation.notTriggered();
        }

        double thresholdKm = riskConfig.getThresholds().getLocationDistanceKm();
        List<Transaction> recent = context.getRecentTransactions();

        if (recent != null) {
            for (Transaction past : recent) {
                if (past.getLatitude() == null || past.getLongitude() == null) continue;

                double distance = GeoUtil.distanceKm(
                        past.getLatitude(), past.getLongitude(),
                        current.getLatitude(), current.getLongitude());

                if (distance > thresholdKm) {
                    return RuleEvaluation.triggered(
                            riskConfig.getScoring().getLocationAnomalyScore(),
                            String.format("Suspicious location change: %.0f km from recent transaction", distance)
                    );
                }
            }
        }

        return RuleEvaluation.notTriggered();
    }

    @Override
    public String getName() {
        return "LocationAnomalyRule";
    }
}
