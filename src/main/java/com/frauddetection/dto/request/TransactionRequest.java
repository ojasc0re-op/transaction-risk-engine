package com.frauddetection.dto.request;

import com.frauddetection.entity.Transaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        @Digits(integer = 13, fraction = 2, message = "Invalid amount format")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        @NotBlank(message = "Merchant is required")
        @Size(max = 200, message = "Merchant name must not exceed 200 characters")
        String merchant,

        @NotNull(message = "Transaction type is required")
        Transaction.TransactionType transactionType,

        Instant transactionTimestamp,

        @Size(max = 200, message = "Location must not exceed 200 characters")
        String location,

        Double latitude,

        Double longitude,

        @Size(max = 100, message = "Device ID must not exceed 100 characters")
        String deviceId,

        @Size(max = 45, message = "IP address must not exceed 45 characters")
        String ipAddress
) {}
