package com.frauddetection.service;

import com.frauddetection.dto.response.DashboardStatsResponse;
import com.frauddetection.dto.response.TransactionResponse;
import com.frauddetection.entity.Transaction;
import com.frauddetection.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    public DashboardStatsResponse getStats() {
        long total = transactionRepository.count();
        long approved = transactionRepository.countByStatus(Transaction.TransactionStatus.APPROVED);
        long blocked = transactionRepository.countByStatus(Transaction.TransactionStatus.BLOCKED);
        long review = transactionRepository.countByStatus(Transaction.TransactionStatus.REVIEW);
        long highRisk = transactionRepository.countHighRisk();
        double avgRiskScore = transactionRepository.findAverageRiskScore().orElse(0.0);

        Map<String, Long> riskDistribution = new HashMap<>();
        transactionRepository.countByRiskLevel().forEach(row ->
                riskDistribution.put(row[0].toString(), (Long) row[1]));

        return new DashboardStatsResponse(
                total, approved, blocked, review,
                Math.round(avgRiskScore * 10.0) / 10.0,
                highRisk, riskDistribution
        );
    }

    public List<TransactionResponse> getRecentSuspicious(int limit) {
        return transactionService.getRecentSuspicious(limit);
    }
}
