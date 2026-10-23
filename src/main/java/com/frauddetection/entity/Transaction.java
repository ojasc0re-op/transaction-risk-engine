package com.frauddetection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions",
       indexes = {
           @Index(name = "idx_txn_user_id", columnList = "user_id"),
           @Index(name = "idx_txn_status", columnList = "status"),
           @Index(name = "idx_txn_risk_level", columnList = "risk_level"),
           @Index(name = "idx_txn_timestamp", columnList = "transaction_timestamp"),
           @Index(name = "idx_txn_device_id", columnList = "device_id")
       })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 200)
    private String merchant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransactionType transactionType;

    @Column(nullable = false)
    private Instant transactionTimestamp;

    @Column(length = 200)
    private String location;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length = 100)
    private String deviceId;

    @Column(length = 45)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.APPROVED;

    @Column(nullable = false)
    @Builder.Default
    private int riskScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RiskLevel riskLevel = RiskLevel.LOW;

    /** Stored as serialized JSON string of triggered rule explanations */
    @Column(columnDefinition = "TEXT")
    private String riskReasons;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public enum TransactionType {
        PURCHASE, WITHDRAWAL, TRANSFER, REFUND, DEPOSIT
    }

    public enum TransactionStatus {
        APPROVED, REVIEW, BLOCKED
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
