package com.paklog.wes.orchestration.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Distributed lock service using Redis
 * Provides distributed locking mechanism for workflow coordination
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String LOCK_PREFIX = "workflow:lock:";

    /**
     * Try to acquire a distributed lock
     *
     * @param workflowId The workflow ID
     * @param timeoutSeconds Lock timeout in seconds
     * @return true if lock was acquired, false otherwise
     */
    public boolean acquireLock(String workflowId, long timeoutSeconds) {
        String lockKey = LOCK_PREFIX + workflowId;
        String lockValue = Thread.currentThread().getName() + ":" + System.currentTimeMillis();

        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, timeoutSeconds, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(acquired)) {
            log.debug("Lock acquired for workflow: {}", workflowId);
            return true;
        }

        log.warn("Failed to acquire lock for workflow: {}", workflowId);
        return false;
    }

    /**
     * Release a distributed lock
     *
     * @param workflowId The workflow ID
     */
    public void releaseLock(String workflowId) {
        String lockKey = LOCK_PREFIX + workflowId;
        Boolean deleted = redisTemplate.delete(lockKey);

        if (Boolean.TRUE.equals(deleted)) {
            log.debug("Lock released for workflow: {}", workflowId);
        } else {
            log.warn("Failed to release lock for workflow: {} - key not found", workflowId);
        }
    }

    /**
     * Extend a lock's expiration time
     *
     * @param workflowId The workflow ID
     * @param additionalSeconds Additional seconds to extend the lock
     * @return true if lock was extended, false otherwise
     */
    public boolean extendLock(String workflowId, long additionalSeconds) {
        String lockKey = LOCK_PREFIX + workflowId;

        // Check if lock exists
        Boolean exists = redisTemplate.hasKey(lockKey);
        if (Boolean.TRUE.equals(exists)) {
            Boolean extended = redisTemplate.expire(lockKey, additionalSeconds, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(extended)) {
                log.debug("Lock extended for workflow: {} by {} seconds", workflowId, additionalSeconds);
                return true;
            }
        }

        log.warn("Failed to extend lock for workflow: {} - lock does not exist", workflowId);
        return false;
    }

    /**
     * Check if a lock exists for a workflow
     *
     * @param workflowId The workflow ID
     * @return true if lock exists, false otherwise
     */
    public boolean isLocked(String workflowId) {
        String lockKey = LOCK_PREFIX + workflowId;
        Boolean exists = redisTemplate.hasKey(lockKey);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Get remaining TTL for a lock
     *
     * @param workflowId The workflow ID
     * @return TTL in seconds, -1 if lock doesn't exist
     */
    public long getLockTTL(String workflowId) {
        String lockKey = LOCK_PREFIX + workflowId;
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        return ttl != null ? ttl : -1;
    }
}
