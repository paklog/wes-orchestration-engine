package com.paklog.wes.orchestration.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Retry policy configuration for workflow steps
 * Implements exponential backoff strategy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryPolicy implements Serializable {

    @Builder.Default
    private Integer maxRetries = 3;

    @Builder.Default
    private Long initialDelayMs = 1000L; // 1 second

    @Builder.Default
    private Long maxDelayMs = 10000L; // 10 seconds

    @Builder.Default
    private Double backoffMultiplier = 2.0;

    @Builder.Default
    private Boolean exponentialBackoff = true;

    /**
     * Calculate delay for given retry attempt
     */
    public long calculateDelay(int retryAttempt) {
        if (!exponentialBackoff) {
            return initialDelayMs;
        }

        long delay = (long) (initialDelayMs * Math.pow(backoffMultiplier, retryAttempt));
        return Math.min(delay, maxDelayMs);
    }

    /**
     * Check if more retries are allowed
     */
    public boolean canRetry(int currentRetryCount) {
        return currentRetryCount < maxRetries;
    }

    /**
     * Create default retry policy
     */
    public static RetryPolicy defaultPolicy() {
        return RetryPolicy.builder()
            .maxRetries(3)
            .initialDelayMs(1000L)
            .maxDelayMs(10000L)
            .backoffMultiplier(2.0)
            .exponentialBackoff(true)
            .build();
    }

    /**
     * Create aggressive retry policy for high-priority workflows
     */
    public static RetryPolicy aggressivePolicy() {
        return RetryPolicy.builder()
            .maxRetries(5)
            .initialDelayMs(500L)
            .maxDelayMs(5000L)
            .backoffMultiplier(1.5)
            .exponentialBackoff(true)
            .build();
    }

    /**
     * Create conservative retry policy
     */
    public static RetryPolicy conservativePolicy() {
        return RetryPolicy.builder()
            .maxRetries(2)
            .initialDelayMs(2000L)
            .maxDelayMs(20000L)
            .backoffMultiplier(3.0)
            .exponentialBackoff(true)
            .build();
    }
}
