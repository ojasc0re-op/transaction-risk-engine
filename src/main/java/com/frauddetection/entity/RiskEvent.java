package com.frauddetection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "risk_events",
       indexes = {
           @Index(name = "idx_risk_event_user_id", columnList = "user_id"),
           @Index(name = "idx_risk_event_transaction_id", columnList = "transaction_id"),
           @Index(name = "idx_risk_event_created_at", columnList = "created_at")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventType eventType;

    @Column(nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Transaction.RiskLevel riskLevel;

    @Column(columnDefinition = "TEXT")
    private String triggeredRules;

    @Column(nullable = false)
    private Instant createdAt;

    public enum EventType {
        TRANSACTION_SCORED,
        TRANSACTION_BLOCKED,
        TRANSACTION_FLAGGED_FOR_REVIEW,
        SUSPICIOUS_ACTIVITY_DETECTED
    }
}
