package com.frauddetection.risk;

import com.frauddetection.entity.Transaction;
import com.frauddetection.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Encapsulates all data required by risk rules to evaluate a transaction.
 */
@Getter
@Builder
public class RiskContext {

    /** The incoming transaction being evaluated */
    private final Transaction transaction;

    /** The user initiating the transaction */
    private final User user;

    /** Recent transactions by the user within the evaluation window */
    private final List<Transaction> recentTransactions;

    /** Number of transactions by this user in the frequency window (from Redis) */
    private final long transactionCountInWindow;

    /** Whether the transaction device is new/unseen for this user */
    private final boolean newDevice;

    /** User's baseline historical average transaction amount */
    private final BigDecimal userAvgAmount;

    /** Number of failed/blocked transactions in recent window */
    private final long failedTransactionCount;
}
