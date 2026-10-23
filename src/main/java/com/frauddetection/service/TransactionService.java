package com.frauddetection.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.dto.request.TransactionRequest;
import com.frauddetection.dto.response.PagedResponse;
import com.frauddetection.dto.response.RiskAssessmentResponse;
import com.frauddetection.dto.response.TransactionResponse;
import com.frauddetection.entity.AuditLog;
import com.frauddetection.entity.Device;
import com.frauddetection.entity.RiskEvent;
import com.frauddetection.entity.Transaction;
import com.frauddetection.entity.User;
import com.frauddetection.exception.ResourceNotFoundException;
import com.frauddetection.mapper.TransactionMapper;
import com.frauddetection.repository.DeviceRepository;
import com.frauddetection.repository.RiskEventRepository;
import com.frauddetection.repository.TransactionRepository;
import com.frauddetection.risk.RiskResult;
import com.frauddetection.risk.engine.RiskEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final DeviceRepository deviceRepository;
    private final RiskEventRepository riskEventRepository;
    private final UserService userService;
    private final RiskEngine riskEngine;
    private final AuditService auditService;
    private final TransactionMapper transactionMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getEntityByUsername(currentUsername);

        // Track or register device
        if (request.deviceId() != null && !request.deviceId().isBlank()) {
            trackDevice(user, request.deviceId());
        }

        // Build transaction entity
        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(request.amount())
                .currency(request.currency())
                .merchant(request.merchant())
                .transactionType(request.transactionType())
                .transactionTimestamp(request.transactionTimestamp() != null
                        ? request.transactionTimestamp() : Instant.now())
                .location(request.location())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .deviceId(request.deviceId())
                .ipAddress(request.ipAddress())
                .build();

        // Run Rule-based Risk Engine
        RiskResult riskResult = riskEngine.evaluate(transaction, user);

        // Apply risk outcome
        transaction.setRiskScore(riskResult.score());
        transaction.setRiskLevel(riskResult.level());
        transaction.setStatus(riskResult.decision());
        transaction.setRiskReasons(serializeReasons(riskResult.reasons()));

        Transaction saved = transactionRepository.save(transaction);

        // Save RiskEvent
        persistRiskEvent(user, saved, riskResult);

        // Save Audit Log
        AuditLog.AuditEventType auditType = switch (riskResult.decision()) {
            case BLOCKED -> AuditLog.AuditEventType.TRANSACTION_BLOCKED;
            case REVIEW -> AuditLog.AuditEventType.TRANSACTION_FLAGGED_FOR_REVIEW;
            case APPROVED -> AuditLog.AuditEventType.TRANSACTION_APPROVED;
        };
        auditService.logTransaction(auditType, user.getUsername(), user.getId(), saved.getId());

        return transactionMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id.toString()));
        userService.assertCanAccessUserData(transaction.getUser().getId());
        return transactionMapper.toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getTransactionsByUser(UUID userId, Pageable pageable) {
        userService.assertCanAccessUserData(userId);
        Page<TransactionResponse> page = transactionRepository
                .findByUserId(userId, pageable)
                .map(transactionMapper::toResponse);
        return PagedResponse.of(page);
    }

    @Transactional(readOnly = true)
    public RiskAssessmentResponse getRiskAssessment(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId.toString()));
        userService.assertCanAccessUserData(transaction.getUser().getId());

        List<String> reasons = deserializeReasons(transaction.getRiskReasons());
        return new RiskAssessmentResponse(
                transaction.getId(),
                transaction.getRiskScore(),
                transaction.getRiskLevel(),
                transaction.getStatus(),
                reasons
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getHighRiskTransactions(Pageable pageable) {
        Page<TransactionResponse> page = transactionRepository
                .findHighRiskTransactions(pageable)
                .map(transactionMapper::toResponse);
        return PagedResponse.of(page);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecentSuspicious(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return transactionRepository.findTopRecentSuspicious(pageable)
                .stream().map(transactionMapper::toResponse).toList();
    }

    private void trackDevice(User user, String deviceId) {
        boolean exists = deviceRepository.existsByUserIdAndDeviceFingerprint(user.getId(), deviceId);
        if (!exists) {
            Device device = Device.builder()
                    .user(user)
                    .deviceFingerprint(deviceId)
                    .trusted(false)
                    .lastSeenAt(Instant.now())
                    .build();
            deviceRepository.save(device);
            log.info("New device fingerprinted for user {}: {}", user.getUsername(), deviceId);
        } else {
            deviceRepository.findByUserIdAndDeviceFingerprint(user.getId(), deviceId)
                    .ifPresent(d -> {
                        d.setLastSeenAt(Instant.now());
                        deviceRepository.save(d);
                    });
        }
    }

    private void persistRiskEvent(User user, Transaction txn, RiskResult riskResult) {
        RiskEvent.EventType eventType = switch (riskResult.decision()) {
            case BLOCKED -> RiskEvent.EventType.TRANSACTION_BLOCKED;
            case REVIEW -> RiskEvent.EventType.TRANSACTION_FLAGGED_FOR_REVIEW;
            case APPROVED -> RiskEvent.EventType.TRANSACTION_SCORED;
        };

        RiskEvent riskEvent = RiskEvent.builder()
                .user(user)
                .transaction(txn)
                .eventType(eventType)
                .riskScore(riskResult.score())
                .riskLevel(riskResult.level())
                .triggeredRules(serializeReasons(riskResult.reasons()))
                .createdAt(Instant.now())
                .build();
        riskEventRepository.save(riskEvent);
    }

    private String serializeReasons(List<String> reasons) {
        try {
            return objectMapper.writeValueAsString(reasons);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> deserializeReasons(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
