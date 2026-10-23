package com.frauddetection.repository;

import com.frauddetection.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);

    Page<AuditLog> findByEventType(AuditLog.AuditEventType eventType, Pageable pageable);

    long countByEventType(AuditLog.AuditEventType eventType);
}
