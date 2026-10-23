package com.frauddetection.risk;

/**
 * Strategy interface for individual risk evaluation rules.
 * Each rule examines specific transaction signals and returns a score contribution.
 */
public interface RiskRule {

    /**
     * Evaluates the rule against the transaction risk context.
     *
     * @param context all collected signal data
     * @return rule evaluation with score added and reason if triggered
     */
    RuleEvaluation evaluate(RiskContext context);

    /**
     * Human-readable rule name.
     */
    String getName();

    /**
     * Immutable result of a single rule evaluation.
     */
    record RuleEvaluation(int scoreAdded, boolean triggered, String reason) {
        public static RuleEvaluation notTriggered() {
            return new RuleEvaluation(0, false, null);
        }

        public static RuleEvaluation triggered(int score, String reason) {
            return new RuleEvaluation(score, true, reason);
        }
    }
}
