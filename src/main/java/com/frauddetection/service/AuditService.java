package com.frauddetection.service;

import com.frauddetection.entity.AuditLog;
import com.frauddetection.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(AuditLog.AuditEventType eventType,
                    String actorUsername,
                    UUID userId,
                    UUID transactionId,
                    String metadata,
                    String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .eventType(eventType)
                    .actorUsername(actorUsername)
                    .userId(userId)
                    .transactionId(transactionId)
                    .metadata(metadata)
                    .ipAddress(ipAddress)
                    .createdAt(Instant.now())
                    .build();
            auditLogRepository.save(auditLog);
            log.info("Audit event recorded: {} for actor: {}", eventType, actorUsername);
        } catch (Exception e) {
            log.error("Failed to persist audit log for event {}: {}", eventType, e.getMessage());
        }
    }

    @Async
    public void logSimple(AuditLog.AuditEventType eventType, String actorUsername, UUID userId) {
        log(eventType, actorUsername, userId, null, null, null);
    }

    @Async
    public void logTransaction(AuditLog.AuditEventType eventType,
                               String actorUsername,
                               UUID userId,
                               UUID transactionId) {
        log(eventType, actorUsername, userId, transactionId, null, null);
    }
}
