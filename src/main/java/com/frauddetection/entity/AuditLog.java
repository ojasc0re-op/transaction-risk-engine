package com.frauddetection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs",
       indexes = {
           @Index(name = "idx_audit_user_id", columnList = "user_id"),
           @Index(name = "idx_audit_event_type", columnList = "event_type"),
           @Index(name = "idx_audit_created_at", columnList = "created_at")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private AuditEventType eventType;

    @Column(length = 100)
    private String actorUsername;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(length = 45)
    private String ipAddress;

    @Column(nullable = false)
    private Instant createdAt;

    public enum AuditEventType {
        USER_REGISTERED,
        USER_LOGIN,
        USER_LOGIN_FAILED,
        USER_ACCOUNT_LOCKED,
        TRANSACTION_CREATED,
        TRANSACTION_APPROVED,
        TRANSACTION_BLOCKED,
        TRANSACTION_FLAGGED_FOR_REVIEW,
        ADMIN_ACTION,
        RISK_SCORE_CALCULATED
    }
}
