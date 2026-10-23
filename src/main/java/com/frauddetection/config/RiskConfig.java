package com.frauddetection.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Centralized configuration for the risk scoring engine.
 * All weights and thresholds are externalized to application.yml
 * so they can be adjusted without code changes.
 */
@Configuration
@ConfigurationProperties(prefix = "app.risk")
@Getter
@Setter
public class RiskConfig {

    private Scoring scoring = new Scoring();
    private Thresholds thresholds = new Thresholds();
    private Levels levels = new Levels();
    private List<String> suspiciousIps = List.of();

    @Getter
    @Setter
    public static class Scoring {
        private int largeTransactionScore = 20;
        private int frequencyAnomalyScore = 20;
        private int newDeviceScore = 15;
        private int locationAnomalyScore = 25;
        private int suspiciousIpScore = 20;
        private int highRiskMerchantScore = 15;
        private int failedTransactionScore = 10;
    }

    @Getter
    @Setter
    public static class Thresholds {
        private double largeTransactionMultiplier = 3.0;
        private int frequencyWindowMinutes = 60;
        private int frequencyMaxCount = 10;
        private double locationDistanceKm = 500.0;
        private int failedTransactionWindowMinutes = 30;
        private int failedTransactionMaxCount = 3;
    }

    @Getter
    @Setter
    public static class Levels {
        private int lowMax = 29;
        private int mediumMax = 59;
        private int highMax = 79;
    }
}
