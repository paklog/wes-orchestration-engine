package com.paklog.wes.orchestration.domain.service;

import com.paklog.wes.orchestration.domain.aggregate.WorkflowInstance;
import com.paklog.wes.orchestration.domain.valueobject.WorkflowPriority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Waveless Processing Domain Service
 *
 * Manages continuous order flow without wave batching.
 * Implements dynamic batching and priority-based processing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WavelessProcessingService {

    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final long DEFAULT_PROCESSING_INTERVAL_MS = 1000L; // 1 second

    /**
     * Enable waveless processing for workflow
     */
    public void enableWaveless(WorkflowInstance workflow) {
        log.info("Enabling waveless processing for workflow: {}", workflow.getId());

        if (!workflow.canTransitionToWaveless()) {
            throw new IllegalStateException(
                "Workflow cannot transition to waveless processing. Type: " +
                    workflow.getWorkflowType() + ", Status: " + workflow.getStatus()
            );
        }

        workflow.transitionToWaveless(DEFAULT_BATCH_SIZE, DEFAULT_PROCESSING_INTERVAL_MS);
    }

    /**
     * Create dynamic batch based on priority
     */
    public List<WorkflowInstance> createDynamicBatch(List<WorkflowInstance> pendingWorkflows,
                                                      int batchSize) {
        log.debug("Creating dynamic batch from {} pending workflows", pendingWorkflows.size());

        // Sort by priority (HIGH > NORMAL > LOW)
        List<WorkflowInstance> sortedWorkflows = pendingWorkflows.stream()
            .sorted(Comparator.comparing(
                WorkflowInstance::getPriority,
                Comparator.comparing(WorkflowPriority::getLevel)
            ))
            .limit(batchSize)
            .collect(Collectors.toList());

        log.info("Created batch of {} workflows. HIGH: {}, NORMAL: {}, LOW: {}",
            sortedWorkflows.size(),
            countByPriority(sortedWorkflows, WorkflowPriority.HIGH),
            countByPriority(sortedWorkflows, WorkflowPriority.NORMAL),
            countByPriority(sortedWorkflows, WorkflowPriority.LOW)
        );

        return sortedWorkflows;
    }

    /**
     * Calculate optimal batch size based on system load
     */
    public int calculateOptimalBatchSize(double systemLoad) {
        if (systemLoad >= 95.0) {
            // System is overloaded, reduce batch size
            return Math.max(1, DEFAULT_BATCH_SIZE / 4);
        } else if (systemLoad >= 85.0) {
            // System is at target, reduce batch size slightly
            return Math.max(1, DEFAULT_BATCH_SIZE / 2);
        } else if (systemLoad < 50.0) {
            // System is underutilized, increase batch size
            return DEFAULT_BATCH_SIZE * 2;
        } else {
            // Normal load, use default batch size
            return DEFAULT_BATCH_SIZE;
        }
    }

    /**
     * Calculate processing interval based on throughput
     */
    public long calculateProcessingInterval(int queueDepth, double throughput) {
        if (queueDepth > 100) {
            // High queue depth, process more frequently
            return 500L; // 0.5 seconds
        } else if (queueDepth > 50) {
            // Moderate queue depth
            return DEFAULT_PROCESSING_INTERVAL_MS;
        } else if (queueDepth < 10) {
            // Low queue depth, can process less frequently
            return 2000L; // 2 seconds
        } else {
            return DEFAULT_PROCESSING_INTERVAL_MS;
        }
    }

    /**
     * Check if workflow should be processed immediately
     */
    public boolean shouldProcessImmediately(WorkflowInstance workflow) {
        // High priority workflows are processed immediately
        if (workflow.getPriority() == WorkflowPriority.HIGH) {
            log.info("Workflow {} marked for immediate processing (HIGH priority)",
                workflow.getId());
            return true;
        }

        // Check if workflow has been waiting too long
        if (workflow.getCreatedAt() != null) {
            long waitingTimeMs = System.currentTimeMillis() -
                workflow.getCreatedAt().toEpochMilli();

            if (waitingTimeMs > 60000L) { // 1 minute
                log.info("Workflow {} marked for immediate processing (waited {} ms)",
                    workflow.getId(), waitingTimeMs);
                return true;
            }
        }

        return false;
    }

    /**
     * Prioritize workflows in queue
     */
    public Queue<WorkflowInstance> prioritizeQueue(List<WorkflowInstance> workflows) {
        log.debug("Prioritizing queue with {} workflows", workflows.size());

        // Create priority queue
        PriorityQueue<WorkflowInstance> priorityQueue = new PriorityQueue<>(
            Comparator.comparing(WorkflowInstance::getPriority,
                Comparator.comparing(WorkflowPriority::getLevel))
                .thenComparing(WorkflowInstance::getCreatedAt)
        );

        priorityQueue.addAll(workflows);

        log.debug("Priority queue created with {} workflows", priorityQueue.size());
        return priorityQueue;
    }

    /**
     * Get waveless processing metrics
     */
    public Map<String, Object> getWavelessMetrics(List<WorkflowInstance> workflows) {
        Map<String, Object> metrics = new HashMap<>();

        long totalWorkflows = workflows.size();
        long highPriority = countByPriority(workflows, WorkflowPriority.HIGH);
        long normalPriority = countByPriority(workflows, WorkflowPriority.NORMAL);
        long lowPriority = countByPriority(workflows, WorkflowPriority.LOW);

        long activeWorkflows = workflows.stream()
            .filter(WorkflowInstance::isActive)
            .count();

        metrics.put("totalWorkflows", totalWorkflows);
        metrics.put("highPriority", highPriority);
        metrics.put("normalPriority", normalPriority);
        metrics.put("lowPriority", lowPriority);
        metrics.put("activeWorkflows", activeWorkflows);
        metrics.put("queueDepth", totalWorkflows - activeWorkflows);

        double avgProgress = workflows.stream()
            .mapToDouble(WorkflowInstance::getProgressPercentage)
            .average()
            .orElse(0.0);
        metrics.put("averageProgress", avgProgress);

        return metrics;
    }

    /**
     * Check if waveless processing should be paused
     */
    public boolean shouldPauseWavelessProcessing(double systemLoad, double errorRate) {
        if (systemLoad >= 95.0) {
            log.warn("System overloaded ({}%). Pausing waveless processing.", systemLoad);
            return true;
        }

        if (errorRate >= 0.5) {
            log.warn("High error rate ({}%). Pausing waveless processing.", errorRate);
            return true;
        }

        return false;
    }

    /**
     * Get recommended batch size
     */
    public int getRecommendedBatchSize(Map<String, Object> metrics) {
        double systemLoad = (double) metrics.getOrDefault("systemLoad", 50.0);
        int queueDepth = (int) metrics.getOrDefault("queueDepth", 0);

        int batchSize = calculateOptimalBatchSize(systemLoad);

        // Adjust based on queue depth
        if (queueDepth > 100) {
            batchSize = Math.min(batchSize * 2, 50); // Cap at 50
        } else if (queueDepth < 10) {
            batchSize = Math.max(batchSize / 2, 5); // Floor at 5
        }

        log.debug("Recommended batch size: {} (systemLoad: {}%, queueDepth: {})",
            batchSize, systemLoad, queueDepth);

        return batchSize;
    }

    // Private helper methods

    private long countByPriority(List<WorkflowInstance> workflows, WorkflowPriority priority) {
        return workflows.stream()
            .filter(w -> w.getPriority() == priority)
            .count();
    }
}
