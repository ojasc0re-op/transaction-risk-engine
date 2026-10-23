package com.frauddetection.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis is utilized for high-throughput, low-latency state tracking:
 * 1. Sliding window transaction counters per user
 * 2. Rapid failed transaction counters
 * 3. Rate limiting per IP or user
 * 4. Short-lived risk score caching
 *
 * Degrades gracefully if Redis is momentarily unavailable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisTransactionService {

    private static final String TXN_COUNT_PREFIX = "txn:count:";
    private static final String FAILED_TXN_PREFIX = "txn:failed:";
    private static final String RATE_LIMIT_PREFIX = "rate:";
    private static final String RISK_SCORE_PREFIX = "risk:score:";

    private final RedisTemplate<String, Object> redisTemplate;

    public long incrementTransactionCount(UUID userId, int windowMinutes) {
        String key = TXN_COUNT_PREFIX + userId + ":" + windowMinutes;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(windowMinutes));
            }
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("Redis unavailable for incrementTransactionCount: {}", e.getMessage());
            return 0L;
        }
    }

    public long getTransactionCount(UUID userId, int windowMinutes) {
        String key = TXN_COUNT_PREFIX + userId + ":" + windowMinutes;
        try {
            Object val = redisTemplate.opsForValue().get(key);
            if (val == null) return 0L;
            return Long.parseLong(val.toString());
        } catch (Exception e) {
            log.warn("Redis unavailable for getTransactionCount: {}", e.getMessage());
            return 0L;
        }
    }

    public long incrementFailedTransactionCount(UUID userId, int windowMinutes) {
        String key = FAILED_TXN_PREFIX + userId;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(windowMinutes));
            }
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("Redis unavailable for incrementFailedTransactionCount: {}", e.getMessage());
            return 0L;
        }
    }

    public long getFailedTransactionCount(UUID userId) {
        String key = FAILED_TXN_PREFIX + userId;
        try {
            Object val = redisTemplate.opsForValue().get(key);
            return val != null ? Long.parseLong(val.toString()) : 0L;
        } catch (Exception e) {
            log.warn("Redis unavailable for getFailedTransactionCount: {}", e.getMessage());
            return 0L;
        }
    }

    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
            }
            return count == null || count <= maxRequests;
        } catch (Exception e) {
            log.warn("Redis unavailable for rate limiting: {}", e.getMessage());
            return true;
        }
    }

    public void cacheUserRiskScore(UUID userId, int score) {
        String key = RISK_SCORE_PREFIX + userId;
        try {
            redisTemplate.opsForValue().set(key, score, Duration.ofMinutes(5));
        } catch (Exception e) {
            log.warn("Redis unavailable for caching risk score: {}", e.getMessage());
        }
    }
}
