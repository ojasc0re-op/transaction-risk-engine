package com.frauddetection.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        boolean enabled,
        boolean accountLocked,
        BigDecimal avgTransactionAmount,
        int totalTransactions,
        Set<String> roles,
        Instant createdAt
) {}
