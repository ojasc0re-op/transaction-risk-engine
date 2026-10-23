package com.frauddetection.repository;

import com.frauddetection.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByUserId(UUID userId, Pageable pageable);

    List<Transaction> findByUserIdAndTransactionTimestampAfter(UUID userId, Instant since);

    Page<Transaction> findByStatus(Transaction.TransactionStatus status, Pageable pageable);

    Page<Transaction> findByRiskLevel(Transaction.RiskLevel riskLevel, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.riskLevel IN ('HIGH', 'CRITICAL') ORDER BY t.createdAt DESC")
    Page<Transaction> findHighRiskTransactions(Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.transactionTimestamp > :since ORDER BY t.transactionTimestamp DESC")
    List<Transaction> findRecentByUserId(@Param("userId") UUID userId, @Param("since") Instant since);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status")
    long countByStatus(@Param("status") Transaction.TransactionStatus status);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.riskLevel IN ('HIGH', 'CRITICAL')")
    long countHighRisk();

    @Query("SELECT AVG(t.riskScore) FROM Transaction t")
    Optional<Double> findAverageRiskScore();

    @Query("SELECT t.riskLevel, COUNT(t) FROM Transaction t GROUP BY t.riskLevel")
    List<Object[]> countByRiskLevel();

    @Query("SELECT t FROM Transaction t WHERE t.riskLevel IN ('HIGH', 'CRITICAL') ORDER BY t.createdAt DESC")
    List<Transaction> findTopRecentSuspicious(Pageable pageable);

    @Query("SELECT AVG(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.status = 'APPROVED'")
    Optional<BigDecimal> findAverageAmountByUserId(@Param("userId") UUID userId);

    long countByUserId(UUID userId);
}
