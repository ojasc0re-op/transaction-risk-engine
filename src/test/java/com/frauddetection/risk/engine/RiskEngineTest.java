package com.frauddetection.risk.engine;

import com.frauddetection.config.RiskConfig;
import com.frauddetection.entity.Transaction;
import com.frauddetection.entity.User;
import com.frauddetection.repository.DeviceRepository;
import com.frauddetection.repository.TransactionRepository;
import com.frauddetection.risk.RiskContext;
import com.frauddetection.risk.RiskResult;
import com.frauddetection.risk.RiskRule;
import com.frauddetection.service.RedisTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RiskEngineTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private RedisTransactionService redisTransactionService;

    private RiskConfig riskConfig;
    private List<RiskRule> mockRules;
    private User testUser;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        riskConfig = new RiskConfig();
        mockRules = new ArrayList<>();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .avgTransactionAmount(new BigDecimal("100.00"))
                .totalTransactions(10)
                .build();

        testTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .merchant("Amazon")
                .transactionType(Transaction.TransactionType.PURCHASE)
                .transactionTimestamp(Instant.now())
                .deviceId("device-001")
                .ipAddress("192.168.1.1")
                .build();

        when(deviceRepository.existsByUserIdAndDeviceFingerprint(any(), any())).thenReturn(true);
        when(redisTransactionService.getTransactionCount(any(), anyInt())).thenReturn(1L);
        when(redisTransactionService.getFailedTransactionCount(any())).thenReturn(0L);
    }

    @Test
    @DisplayName("Normal transaction with no triggered rules yields LOW risk score 0 and APPROVED status")
    void testNormalTransactionApproved() {
        RiskRule benignRule = mock(RiskRule.class);
        when(benignRule.evaluate(any(RiskContext.class))).thenReturn(RiskRule.RuleEvaluation.notTriggered());
        when(benignRule.getName()).thenReturn("BenignRule");
        mockRules.add(benignRule);

        RiskEngine engine = new RiskEngine(mockRules, riskConfig, transactionRepository, deviceRepository, redisTransactionService);
        RiskResult result = engine.evaluate(testTransaction, testUser);

        assertThat(result.score()).isEqualTo(0);
        assertThat(result.level()).isEqualTo(Transaction.RiskLevel.LOW);
        assertThat(result.decision()).isEqualTo(Transaction.TransactionStatus.APPROVED);
        assertThat(result.reasons()).isEmpty();

        verify(redisTransactionService).incrementTransactionCount(eq(testUser.getId()), anyInt());
        verify(redisTransactionService, never()).incrementFailedTransactionCount(any(), anyInt());
    }

    @Test
    @DisplayName("Single rule triggered adds expected score and explanation")
    void testSingleRuleTriggered() {
        RiskRule rule = mock(RiskRule.class);
        when(rule.evaluate(any(RiskContext.class))).thenReturn(
                RiskRule.RuleEvaluation.triggered(20, "Unusual transaction amount")
        );
        when(rule.getName()).thenReturn("LargeTransactionRule");
        mockRules.add(rule);

        RiskEngine engine = new RiskEngine(mockRules, riskConfig, transactionRepository, deviceRepository, redisTransactionService);
        RiskResult result = engine.evaluate(testTransaction, testUser);

        assertThat(result.score()).isEqualTo(20);
        assertThat(result.level()).isEqualTo(Transaction.RiskLevel.LOW);
        assertThat(result.decision()).isEqualTo(Transaction.TransactionStatus.APPROVED);
        assertThat(result.reasons()).containsExactly("Unusual transaction amount");
    }

    @Test
    @DisplayName("Medium risk score (30-59) yields MEDIUM level and APPROVED status")
    void testMediumRiskLevel() {
        RiskRule rule1 = mock(RiskRule.class);
        RiskRule rule2 = mock(RiskRule.class);
        when(rule1.evaluate(any(RiskContext.class))).thenReturn(RiskRule.RuleEvaluation.triggered(20, "Frequency anomaly"));
        when(rule2.evaluate(any(RiskContext.class))).thenReturn(RiskRule.RuleEvaluation.triggered(20, "Large amount"));
        when(rule1.getName()).thenReturn("Rule1");
        when(rule2.getName()).thenReturn("Rule2");
        mockRules.addAll(List.of(rule1, rule2));

        RiskEngine engine = new RiskEngine(mockRules, riskConfig, transactionRepository, deviceRepository, redisTransactionService);
        RiskResult result = engine.evaluate(testTransaction, testUser);

        assertThat(result.score()).isEqualTo(40);
        assertThat(result.level()).isEqualTo(Transaction.RiskLevel.MEDIUM);
        assertThat(result.decision()).isEqualTo(Transaction.TransactionStatus.APPROVED);
        assertThat(result.reasons()).hasSize(2);
    }

    @Test
    @DisplayName("High risk score (60-79) yields HIGH level and REVIEW decision")
    void testHighRiskLevelReview() {
        RiskRule rule1 = mock(RiskRule.class);
        RiskRule rule2 = mock(RiskRule.class);
        RiskRule rule3 = mock(RiskRule.class);
        when(rule1.evaluate(any(RiskContext.class))).thenReturn(RiskRule.RuleEvaluation.triggered(25, "Location anomaly"));
        when(rule2.evaluate(any(RiskContext.class))).thenReturn(RiskRule.RuleEvaluation.triggered(20, "Suspicious IP"));
        when(rule3.evaluate(any(RiskContext.class))).thenReturn(RiskRule.RuleEvaluation.triggered(20, "Large amount"));
        when(rule1.getName()).thenReturn("Rule1");
        when(rule2.getName()).thenReturn("Rule2");
        when(rule3.getName()).thenReturn("Rule3");
        mockRules.addAll(List.of(rule1, rule2, rule3));

        RiskEngine engine = new RiskEngine(mockRules, riskConfig, transactionRepository, deviceRepository, redisTransactionService);
        RiskResult result = engine.evaluate(testTransaction, testUser);

        assertThat(result.score()).isEqualTo(65);
        assertThat(result.level()).isEqualTo(Transaction.RiskLevel.HIGH);
        assertThat(result.decision()).isEqualTo(Transaction.TransactionStatus.REVIEW);
    }

    @Test
    @DisplayName("Critical risk score (80-100) yields CRITICAL level, BLOCKED status, and increments failed counter")
    void testCriticalRiskLevelBlocked() {
        RiskRule rule = mock(RiskRule.class);
        when(rule.evaluate(any(RiskContext.class))).thenReturn(
                RiskRule.RuleEvaluation.triggered(85, "Multiple severe fraud indicators")
        );
        when(rule.getName()).thenReturn("SevereRule");
        mockRules.add(rule);

        RiskEngine engine = new RiskEngine(mockRules, riskConfig, transactionRepository, deviceRepository, redisTransactionService);
        RiskResult result = engine.evaluate(testTransaction, testUser);

        assertThat(result.score()).isEqualTo(85);
        assertThat(result.level()).isEqualTo(Transaction.RiskLevel.CRITICAL);
        assertThat(result.decision()).isEqualTo(Transaction.TransactionStatus.BLOCKED);

        // Verifies failed transaction counter is incremented on block
        verify(redisTransactionService).incrementFailedTransactionCount(eq(testUser.getId()), anyInt());
    }

    @Test
    @DisplayName("Score normalization caps raw scores above 100 to exactly 100")
    void testScoreCappedAt100() {
        RiskRule rule1 = mock(RiskRule.class);
        RiskRule rule2 = mock(RiskRule.class);
        when(rule1.evaluate(any(RiskContext.class))).thenReturn(RiskRule.RuleEvaluation.triggered(60, "Severe 1"));
        when(rule2.evaluate(any(RiskContext.class))).thenReturn(RiskRule.RuleEvaluation.triggered(70, "Severe 2"));
        when(rule1.getName()).thenReturn("Rule1");
        when(rule2.getName()).thenReturn("Rule2");
        mockRules.addAll(List.of(rule1, rule2));

        RiskEngine engine = new RiskEngine(mockRules, riskConfig, transactionRepository, deviceRepository, redisTransactionService);
        RiskResult result = engine.evaluate(testTransaction, testUser);

        assertThat(result.score()).isEqualTo(100);
        assertThat(result.level()).isEqualTo(Transaction.RiskLevel.CRITICAL);
        assertThat(result.decision()).isEqualTo(Transaction.TransactionStatus.BLOCKED);
    }

    @Test
    @DisplayName("Score boundary condition check: 29 -> LOW, 30 -> MEDIUM, 59 -> MEDIUM, 60 -> HIGH, 79 -> HIGH, 80 -> CRITICAL")
    void testBoundaryConditions() {
        RiskEngine engine = new RiskEngine(mockRules, riskConfig, transactionRepository, deviceRepository, redisTransactionService);

        assertThat(engine.classifyRiskLevel(0)).isEqualTo(Transaction.RiskLevel.LOW);
        assertThat(engine.classifyRiskLevel(29)).isEqualTo(Transaction.RiskLevel.LOW);
        assertThat(engine.classifyRiskLevel(30)).isEqualTo(Transaction.RiskLevel.MEDIUM);
        assertThat(engine.classifyRiskLevel(59)).isEqualTo(Transaction.RiskLevel.MEDIUM);
        assertThat(engine.classifyRiskLevel(60)).isEqualTo(Transaction.RiskLevel.HIGH);
        assertThat(engine.classifyRiskLevel(79)).isEqualTo(Transaction.RiskLevel.HIGH);
        assertThat(engine.classifyRiskLevel(80)).isEqualTo(Transaction.RiskLevel.CRITICAL);
        assertThat(engine.classifyRiskLevel(100)).isEqualTo(Transaction.RiskLevel.CRITICAL);
    }
}
