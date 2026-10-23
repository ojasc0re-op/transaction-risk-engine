package com.frauddetection.repository;

import com.frauddetection.entity.RiskEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiskEventRepository extends JpaRepository<RiskEvent, UUID> {

    List<RiskEvent> findByTransactionId(UUID transactionId);

    List<RiskEvent> findByUserId(UUID userId);

    Optional<RiskEvent> findFirstByTransactionIdOrderByCreatedAtDesc(UUID transactionId);
}
