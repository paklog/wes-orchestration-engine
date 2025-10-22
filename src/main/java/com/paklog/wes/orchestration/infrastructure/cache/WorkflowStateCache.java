package com.paklog.wes.orchestration.infrastructure.cache;

import com.paklog.wes.orchestration.domain.aggregate.WorkflowInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Cache service for workflow state using Redis
 * Caches active workflow states to reduce database load
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowStateCache {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_PREFIX = "workflow:state:";
    private static final long DEFAULT_TTL_MINUTES = 5;

    /**
     * Get workflow state from cache
     *
     * @param workflowId The workflow ID
     * @return The cached workflow instance, or null if not found
     */
    public WorkflowInstance get(String workflowId) {
        String cacheKey = CACHE_PREFIX + workflowId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof WorkflowInstance) {
                log.debug("Cache hit for workflow: {}", workflowId);
                return (WorkflowInstance) cached;
            }
        } catch (Exception e) {
            log.error("Error retrieving workflow from cache: {}", workflowId, e);
        }
        log.debug("Cache miss for workflow: {}", workflowId);
        return null;
    }

    /**
     * Put workflow state into cache
     *
     * @param workflow The workflow instance to cache
     */
    public void put(WorkflowInstance workflow) {
        if (workflow == null || workflow.getId() == null) {
            log.warn("Cannot cache null workflow or workflow without ID");
            return;
        }

        String cacheKey = CACHE_PREFIX + workflow.getId();
        try {
            redisTemplate.opsForValue().set(
                cacheKey,
                workflow,
                DEFAULT_TTL_MINUTES,
                TimeUnit.MINUTES
            );
            log.debug("Cached workflow: {}", workflow.getId());
        } catch (Exception e) {
            log.error("Error caching workflow: {}", workflow.getId(), e);
        }
    }

    /**
     * Put workflow state into cache with custom TTL
     *
     * @param workflow The workflow instance to cache
     * @param ttlMinutes Time to live in minutes
     */
    public void put(WorkflowInstance workflow, long ttlMinutes) {
        if (workflow == null || workflow.getId() == null) {
            log.warn("Cannot cache null workflow or workflow without ID");
            return;
        }

        String cacheKey = CACHE_PREFIX + workflow.getId();
        try {
            redisTemplate.opsForValue().set(
                cacheKey,
                workflow,
                ttlMinutes,
                TimeUnit.MINUTES
            );
            log.debug("Cached workflow: {} with TTL: {} minutes", workflow.getId(), ttlMinutes);
        } catch (Exception e) {
            log.error("Error caching workflow: {}", workflow.getId(), e);
        }
    }

    /**
     * Evict workflow state from cache
     *
     * @param workflowId The workflow ID
     */
    public void evict(String workflowId) {
        String cacheKey = CACHE_PREFIX + workflowId;
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Evicted workflow from cache: {}", workflowId);
            } else {
                log.debug("Workflow not found in cache for eviction: {}", workflowId);
            }
        } catch (Exception e) {
            log.error("Error evicting workflow from cache: {}", workflowId, e);
        }
    }

    /**
     * Check if workflow exists in cache
     *
     * @param workflowId The workflow ID
     * @return true if workflow is cached, false otherwise
     */
    public boolean exists(String workflowId) {
        String cacheKey = CACHE_PREFIX + workflowId;
        try {
            Boolean exists = redisTemplate.hasKey(cacheKey);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Error checking cache for workflow: {}", workflowId, e);
            return false;
        }
    }

    /**
     * Clear all workflow caches
     */
    public void clearAll() {
        try {
            var keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} workflow cache entries", keys.size());
            }
        } catch (Exception e) {
            log.error("Error clearing workflow cache", e);
        }
    }
}
