package com.paklog.wes.orchestration.application.service;

import com.paklog.wes.orchestration.application.command.RebalanceSystemLoadCommand;
import com.paklog.wes.orchestration.application.port.out.PublishEventPort;
import com.paklog.wes.orchestration.domain.aggregate.WorkflowInstance;
import com.paklog.wes.orchestration.domain.entity.SystemLoad;
import com.paklog.wes.orchestration.domain.event.DomainEvent;
import com.paklog.wes.orchestration.domain.event.SystemLoadRebalancedEvent;
import com.paklog.wes.orchestration.domain.repository.WorkflowInstanceRepository;
import com.paklog.wes.orchestration.domain.service.WavelessProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Waveless Orchestration Application Service
 * Manages waveless processing operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WavelessOrchestrationService {

    private final WavelessProcessingService wavelessProcessingService;
    private final WorkflowInstanceRepository workflowRepository;
    private final PublishEventPort publishEventPort;

    /**
     * Enable waveless processing for a workflow
     */
    @Transactional
    public void enableWavelessProcessing(String workflowId) {
        log.info("Enabling waveless processing for workflow: {}", workflowId);

        WorkflowInstance workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        wavelessProcessingService.enableWaveless(workflow);

        WorkflowInstance savedWorkflow = workflowRepository.save(workflow);
        publishDomainEvents(savedWorkflow);

        log.info("Waveless processing enabled for workflow: {}", workflowId);
    }

    /**
     * Create dynamic batch from pending workflows
     */
    public List<WorkflowInstance> createBatch(int batchSize) {
        log.debug("Creating dynamic batch with size: {}", batchSize);

        List<WorkflowInstance> pendingWorkflows = workflowRepository.findWorkflowsForWavelessProcessing();

        List<WorkflowInstance> batch = wavelessProcessingService.createDynamicBatch(
            pendingWorkflows,
            batchSize
        );

        log.info("Created batch of {} workflows from {} pending", batch.size(), pendingWorkflows.size());
        return batch;
    }

    /**
     * Process batch of workflows
     */
    @Transactional
    public void processBatch(List<WorkflowInstance> batch) {
        log.info("Processing batch of {} workflows", batch.size());

        for (WorkflowInstance workflow : batch) {
            try {
                // Process workflow
                if (workflow.isActive()) {
                    log.debug("Processing workflow in batch: {}", workflow.getId());
                    // Actual processing logic would be here
                }
            } catch (Exception e) {
                log.error("Error processing workflow {} in batch", workflow.getId(), e);
            }
        }

        log.info("Batch processing completed for {} workflows", batch.size());
    }

    /**
     * Manage priority queue for workflows
     */
    public Queue<WorkflowInstance> managePriorityQueue() {
        log.debug("Managing priority queue");

        List<WorkflowInstance> workflows = workflowRepository.findWorkflowsForWavelessProcessing();
        Queue<WorkflowInstance> priorityQueue = wavelessProcessingService.prioritizeQueue(workflows);

        log.info("Priority queue managed with {} workflows", priorityQueue.size());
        return priorityQueue;
    }

    /**
     * Get waveless processing metrics
     */
    public Map<String, Object> getWavelessMetrics() {
        log.debug("Getting waveless processing metrics");

        List<WorkflowInstance> workflows = workflowRepository.findActiveWorkflows();
        Map<String, Object> metrics = wavelessProcessingService.getWavelessMetrics(workflows);

        log.debug("Waveless metrics: {}", metrics);
        return metrics;
    }

    /**
     * Rebalance system load
     */
    @Transactional
    public void rebalanceSystemLoad(RebalanceSystemLoadCommand command) {
        log.info("Rebalancing system load with target utilization: {}", command.getTargetUtilization());

        List<WorkflowInstance> activeWorkflows = workflowRepository.findActiveWorkflows();

        // Calculate current load
        double currentLoad = calculateCurrentLoad(activeWorkflows);
        log.info("Current system load: {}%", currentLoad);

        if (currentLoad > command.getTargetUtilization() * 100) {
            // System is overloaded, pause some workflows
            int workflowsToPause = calculateWorkflowsToPause(activeWorkflows, command.getTargetUtilization());
            pauseLowPriorityWorkflows(activeWorkflows, workflowsToPause);
        } else if (currentLoad < command.getTargetUtilization() * 100 * 0.7) {
            // System is underutilized, resume paused workflows
            resumePausedWorkflows();
        }

        // Publish rebalance event
        SystemLoadRebalancedEvent event = SystemLoadRebalancedEvent.builder()
            .eventType("SystemLoadRebalanced")
            .aggregateId("system")
            .previousLoad(currentLoad)
            .newLoad(calculateCurrentLoad(workflowRepository.findActiveWorkflows()))
            .targetUtilization(command.getTargetUtilization())
            .rebalancedAt(Instant.now())
            .build();

        publishEventPort.publishEvent(event);

        log.info("System load rebalanced successfully");
    }

    /**
     * Check if waveless processing should be paused
     */
    public boolean shouldPauseWavelessProcessing() {
        List<WorkflowInstance> activeWorkflows = workflowRepository.findActiveWorkflows();
        double systemLoad = calculateCurrentLoad(activeWorkflows);
        double errorRate = calculateErrorRate(activeWorkflows);

        return wavelessProcessingService.shouldPauseWavelessProcessing(systemLoad, errorRate);
    }

    /**
     * Get recommended batch size based on current metrics
     */
    public int getRecommendedBatchSize() {
        Map<String, Object> metrics = getWavelessMetrics();
        List<WorkflowInstance> activeWorkflows = workflowRepository.findActiveWorkflows();
        double systemLoad = calculateCurrentLoad(activeWorkflows);
        metrics.put("systemLoad", systemLoad);

        return wavelessProcessingService.getRecommendedBatchSize(metrics);
    }

    // Private helper methods

    private void publishDomainEvents(WorkflowInstance workflow) {
        List<DomainEvent> events = workflow.getDomainEvents();
        for (DomainEvent event : events) {
            try {
                publishEventPort.publishEvent(event);
            } catch (Exception e) {
                log.error("Failed to publish event: {}", event.getEventType(), e);
            }
        }
        workflow.clearDomainEvents();
    }

    private double calculateCurrentLoad(List<WorkflowInstance> activeWorkflows) {
        if (activeWorkflows.isEmpty()) {
            return 0.0;
        }

        // Calculate average utilization across all workflows
        double totalUtilization = activeWorkflows.stream()
            .mapToDouble(w -> w.calculateSystemLoad().getUtilizationPercentage())
            .average()
            .orElse(0.0);

        return totalUtilization;
    }

    private double calculateErrorRate(List<WorkflowInstance> workflows) {
        if (workflows.isEmpty()) {
            return 0.0;
        }

        long failedWorkflows = workflows.stream()
            .filter(w -> w.getStatus().name().equals("FAILED"))
            .count();

        return (double) failedWorkflows / workflows.size();
    }

    private int calculateWorkflowsToPause(List<WorkflowInstance> workflows, double targetUtilization) {
        int totalWorkflows = workflows.size();
        double currentLoad = calculateCurrentLoad(workflows);
        double targetLoad = targetUtilization * 100;

        if (currentLoad <= targetLoad) {
            return 0;
        }

        // Calculate percentage of workflows to pause
        double excessLoad = currentLoad - targetLoad;
        double pausePercentage = excessLoad / currentLoad;

        return (int) Math.ceil(totalWorkflows * pausePercentage);
    }

    private void pauseLowPriorityWorkflows(List<WorkflowInstance> workflows, int count) {
        workflows.stream()
            .filter(w -> w.getStatus().name().equals("EXECUTING"))
            .sorted((w1, w2) -> w2.getPriority().compareTo(w1.getPriority())) // Sort by priority descending
            .limit(count)
            .forEach(workflow -> {
                try {
                    workflow.pause();
                    workflowRepository.save(workflow);
                    log.info("Paused workflow {} for load balancing", workflow.getId());
                } catch (Exception e) {
                    log.error("Failed to pause workflow {}", workflow.getId(), e);
                }
            });
    }

    private void resumePausedWorkflows() {
        List<WorkflowInstance> pausedWorkflows = workflowRepository.findByStatus(
            com.paklog.wes.orchestration.domain.valueobject.WorkflowStatus.PAUSED
        );

        pausedWorkflows.forEach(workflow -> {
            try {
                workflow.resume();
                workflowRepository.save(workflow);
                log.info("Resumed workflow {} after load rebalancing", workflow.getId());
            } catch (Exception e) {
                log.error("Failed to resume workflow {}", workflow.getId(), e);
            }
        });
    }
}
