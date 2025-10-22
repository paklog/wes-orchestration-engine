package com.paklog.wes.orchestration.domain.entity;

import com.paklog.wes.orchestration.domain.valueobject.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Workflow Step Execution Entity
 *
 * Represents a single step within a workflow instance.
 * Manages step state, execution, retry logic, and compensation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepExecution {

    private String stepId;
    private String workflowInstanceId;
    private String stepName;
    private String stepType;
    private String serviceName;
    private String operation;
    private Integer executionOrder;

    @Builder.Default
    private StepStatus status = StepStatus.PENDING;

    private Map<String, Object> inputData;
    private Map<String, Object> outputData;
    private StepResult result;

    @Builder.Default
    private Integer retriesRemaining = 3;

    @Builder.Default
    private Integer retryCount = 0;

    private RetryPolicy retryPolicy;
    private CompensationAction compensationAction;

    private Long timeoutMs;
    private Instant startedAt;
    private Instant completedAt;
    private Instant executedAt;
    private Long executionDurationMs;

    private Boolean compensated;
    private Instant compensatedAt;

    private WorkflowError error;

    /**
     * Start step execution
     */
    public void execute() {
        if (this.status != StepStatus.PENDING && this.status != StepStatus.FAILED) {
            throw new IllegalStateException("Step can only be executed from PENDING or FAILED status");
        }

        this.status = StepStatus.EXECUTING;
        this.startedAt = Instant.now();

        if (this.status == StepStatus.FAILED) {
            this.retryCount++;
            this.retriesRemaining--;
        }
    }

    /**
     * Mark step as completed
     */
    public void markCompleted(StepResult result) {
        if (this.status != StepStatus.EXECUTING) {
            throw new IllegalStateException("Can only complete executing steps");
        }

        this.status = StepStatus.COMPLETED;
        this.result = result;
        this.outputData = result.getOutputData();
        this.completedAt = Instant.now();
        this.executionDurationMs = calculateDuration();
        this.executedAt = Instant.now();
    }

    /**
     * Mark step as failed
     */
    public void markFailed(WorkflowError error) {
        this.status = StepStatus.FAILED;
        this.error = error;
        this.completedAt = Instant.now();
        this.executionDurationMs = calculateDuration();
    }

    /**
     * Compensate (rollback) step
     */
    public void compensate() {
        if (this.status != StepStatus.COMPLETED) {
            throw new IllegalStateException("Can only compensate completed steps");
        }

        if (this.compensationAction == null) {
            throw new IllegalStateException("No compensation action defined for step: " + stepId);
        }

        this.status = StepStatus.COMPENSATING;
    }

    /**
     * Mark compensation as completed
     */
    public void markCompensated() {
        if (this.status != StepStatus.COMPENSATING) {
            throw new IllegalStateException("Can only mark compensated when compensating");
        }

        this.status = StepStatus.COMPENSATED;
        this.compensated = true;
        this.compensatedAt = Instant.now();
    }

    /**
     * Check if step can be retried
     */
    public boolean canRetry() {
        return this.status == StepStatus.FAILED
            && this.retriesRemaining > 0
            && (this.retryPolicy == null || this.retryPolicy.canRetry(this.retryCount));
    }

    /**
     * Retry failed step
     */
    public void retry() {
        if (!canRetry()) {
            throw new IllegalStateException("Step cannot be retried");
        }

        this.status = StepStatus.PENDING;
        this.error = null;
        this.startedAt = null;
        this.completedAt = null;
    }

    /**
     * Check if step has timed out
     */
    public boolean hasTimedOut() {
        if (this.status != StepStatus.EXECUTING || this.startedAt == null || this.timeoutMs == null) {
            return false;
        }

        long elapsedMs = Instant.now().toEpochMilli() - this.startedAt.toEpochMilli();
        return elapsedMs > this.timeoutMs;
    }

    /**
     * Calculate retry delay
     */
    public long calculateRetryDelay() {
        if (this.retryPolicy == null) {
            return RetryPolicy.defaultPolicy().calculateDelay(this.retryCount);
        }
        return this.retryPolicy.calculateDelay(this.retryCount);
    }

    /**
     * Check if step requires compensation
     */
    public boolean requiresCompensation() {
        return this.status == StepStatus.COMPLETED && this.compensationAction != null;
    }

    /**
     * Skip step execution
     */
    public void skip(String reason) {
        this.status = StepStatus.SKIPPED;
        this.completedAt = Instant.now();
        if (this.outputData == null) {
            this.outputData = Map.of("skipped", true, "reason", reason);
        }
    }

    /**
     * Check if step is terminal
     */
    public boolean isTerminal() {
        return this.status != null && this.status.isTerminal();
    }

    /**
     * Check if step is active
     */
    public boolean isActive() {
        return this.status != null && this.status.isActive();
    }

    // Private helper methods

    private Long calculateDuration() {
        if (startedAt != null && completedAt != null) {
            return completedAt.toEpochMilli() - startedAt.toEpochMilli();
        }
        return null;
    }

    /**
     * Create a step execution with default retry policy
     */
    public static StepExecution create(String stepId, String workflowInstanceId, String stepName,
                                       String serviceName, String operation, int executionOrder,
                                       Map<String, Object> inputData, Long timeoutMs) {
        return StepExecution.builder()
            .stepId(stepId)
            .workflowInstanceId(workflowInstanceId)
            .stepName(stepName)
            .serviceName(serviceName)
            .operation(operation)
            .executionOrder(executionOrder)
            .inputData(inputData)
            .timeoutMs(timeoutMs != null ? timeoutMs : 5000L)
            .retryPolicy(RetryPolicy.defaultPolicy())
            .retriesRemaining(3)
            .retryCount(0)
            .status(StepStatus.PENDING)
            .compensated(false)
            .build();
    }
}
