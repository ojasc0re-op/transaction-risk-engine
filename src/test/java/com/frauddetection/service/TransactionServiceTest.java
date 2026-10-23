package com.frauddetection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.dto.request.TransactionRequest;
import com.frauddetection.dto.response.TransactionResponse;
import com.frauddetection.entity.AuditLog;
import com.frauddetection.entity.Transaction;
import com.frauddetection.entity.User;
import com.frauddetection.mapper.TransactionMapper;
import com.frauddetection.repository.DeviceRepository;
import com.frauddetection.repository.RiskEventRepository;
import com.frauddetection.repository.TransactionRepository;
import com.frauddetection.risk.RiskResult;
import com.frauddetection.risk.engine.RiskEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private RiskEventRepository riskEventRepository;

    @Mock
    private UserService userService;

    @Mock
    private RiskEngine riskEngine;

    @Mock
    private AuditService auditService;

    @Mock
    private TransactionMapper transactionMapper;

    private ObjectMapper objectMapper;
    private TransactionService transactionService;
    private User mockUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        transactionService = new TransactionService(
                transactionRepository,
                deviceRepository,
                riskEventRepository,
                userService,
                riskEngine,
                auditService,
                transactionMapper,
                objectMapper
        );

        mockUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        SecurityContext secContext = mock(SecurityContext.class);
        when(secContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secContext);
    }

    @Test
    @DisplayName("createTransaction delegates to RiskEngine, registers unseen device, and saves transaction")
    void testCreateTransactionEvaluatesRiskAndTracksDevice() {
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("150.00"),
                "USD",
                "Amazon",
                Transaction.TransactionType.PURCHASE,
                Instant.now(),
                "New York, NY",
                40.7128,
                -74.0060,
                "device-new-999",
                "192.168.1.1"
        );

        when(userService.getEntityByUsername("testuser")).thenReturn(mockUser);
        when(deviceRepository.existsByUserIdAndDeviceFingerprint(mockUser.getId(), "device-new-999")).thenReturn(false);

        RiskResult riskResult = new RiskResult(
                15,
                Transaction.RiskLevel.LOW,
                Transaction.TransactionStatus.APPROVED,
                List.of("New device detected")
        );
        when(riskEngine.evaluate(any(Transaction.class), eq(mockUser))).thenReturn(riskResult);

        Transaction savedTxn = Transaction.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .amount(request.amount())
                .currency(request.currency())
                .merchant(request.merchant())
                .transactionType(request.transactionType())
                .status(Transaction.TransactionStatus.APPROVED)
                .riskScore(15)
                .riskLevel(Transaction.RiskLevel.LOW)
                .build();
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTxn);

        TransactionResponse expectedResponse = new TransactionResponse(
                savedTxn.getId(),
                mockUser.getId(),
                "testuser",
                request.amount(),
                request.currency(),
                request.merchant(),
                request.transactionType(),
                savedTxn.getTransactionTimestamp(),
                request.location(),
                request.deviceId(),
                Transaction.TransactionStatus.APPROVED,
                15,
                Transaction.RiskLevel.LOW,
                Instant.now()
        );
        when(transactionMapper.toResponse(savedTxn)).thenReturn(expectedResponse);

        TransactionResponse response = transactionService.createTransaction(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(Transaction.TransactionStatus.APPROVED);
        assertThat(response.riskScore()).isEqualTo(15);

        // Verify new device saved
        verify(deviceRepository).save(any());
        // Verify transaction saved
        verify(transactionRepository).save(any(Transaction.class));
        // Verify risk event logged
        verify(riskEventRepository).save(any());
        // Verify audit log recorded
        verify(auditService).logTransaction(
                eq(AuditLog.AuditEventType.TRANSACTION_APPROVED),
                eq("testuser"),
                eq(mockUser.getId()),
                eq(savedTxn.getId())
        );
    }
}
