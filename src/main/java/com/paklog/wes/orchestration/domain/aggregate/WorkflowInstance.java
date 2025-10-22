package com.paklog.wes.orchestration.domain.aggregate;

import com.paklog.wes.orchestration.domain.entity.StepExecution;
import com.paklog.wes.orchestration.domain.entity.SystemLoad;
import com.paklog.wes.orchestration.domain.event.*;
import com.paklog.wes.orchestration.domain.valueobject.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.*;

/**
 * Workflow Instance Aggregate - Root aggregate for orchestration workflows
 *
 * Represents a running instance of a workflow that orchestrates operations
 * across multiple warehouse systems. Manages state transitions, error handling,
 * and compensation logic for distributed transactions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workflow_instances")
public class WorkflowInstance {

    @Id
    private String id;

    private String workflowDefinitionId;
    private String workflowName;
    private WorkflowType workflowType;
    private WorkflowStatus status;
    private ExecutionContext executionContext;
    private Map<String, StepExecution> steps;
    private List<String> executedSteps;
    private List<String> compensatedSteps;
    private String currentStepId;
    private WorkflowPriority priority;
    private String triggeredBy;
    private String correlationId;
    private Map<String, Object> inputParameters;
    private Map<String, Object> outputParameters;
    private List<WorkflowError> errors;
    private Integer retryCount;
    private Integer maxRetries;
    private Instant startedAt;
    private Instant completedAt;
    private Long executionDurationMs;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * Start workflow execution
     */
    public void start() {
        if (this.status != WorkflowStatus.PENDING) {
            throw new IllegalStateException("Workflow can only be started from PENDING status");
        }

        this.status = WorkflowStatus.EXECUTING;
        this.startedAt = Instant.now();
        this.retryCount = 0;

        addDomainEvent(WorkflowStartedEvent.builder()
            .workflowInstanceId(this.id)
            .workflowDefinitionId(this.workflowDefinitionId)
            .workflowType(this.workflowType)
            .correlationId(this.correlationId)
            .startedAt(this.startedAt)
            .build());
    }

    /**
     * Execute a workflow step (enhanced with saga support)
     */
    public void executeStep(String stepId, StepResult result) {
        if (this.status != WorkflowStatus.EXECUTING) {
            throw new IllegalStateException("Cannot execute step when workflow is not in EXECUTING status");
        }

        StepExecution step = this.steps.get(stepId);
        if (step == null) {
            throw new IllegalArgumentException("Step not found: " + stepId);
        }

        // Mark step as completed
        step.markCompleted(result);

        // Add to executed steps for compensation tracking
        if (this.executedSteps == null) {
            this.executedSteps = new ArrayList<>();
        }
        this.executedSteps.add(stepId);

        // Determine next step
        this.currentStepId = getNextStepId(stepId);

        addDomainEvent(WorkflowStepExecutedEvent.builder()
            .workflowInstanceId(this.id)
            .stepId(stepId)
            .stepName(step.getStepName())
            .result(result)
            .executedAt(step.getCompletedAt())
            .build());
    }

    /**
     * Start executing a specific step
     */
    public void startStepExecution(String stepId) {
        if (this.status != WorkflowStatus.EXECUTING) {
            throw new IllegalStateException("Cannot start step when workflow is not in EXECUTING status");
        }

        StepExecution step = this.steps.get(stepId);
        if (step == null) {
            throw new IllegalArgumentException("Step not found: " + stepId);
        }

        step.execute();
        this.currentStepId = stepId;
    }

    /**
     * Handle step execution failure
     */
    public void handleStepFailure(String stepId, WorkflowError error) {
        StepExecution step = this.steps.get(stepId);
        if (step == null) {
            throw new IllegalArgumentException("Step not found: " + stepId);
        }

        step.markFailed(error);

        // Check if step can be retried
        boolean willRetry = step.canRetry();

        addDomainEvent(WorkflowStepFailedEvent.builder()
            .workflowInstanceId(this.id)
            .stepId(stepId)
            .stepName(step.getStepName())
            .error(error)
            .willRetry(willRetry)
            .retryCount(step.getRetryCount())
            .failedAt(Instant.now())
            .build());

        if (!willRetry && !error.isRecoverable()) {
            // Trigger workflow failure and compensation
            this.fail(error);
        }
    }

    /**
     * Retry a failed step
     */
    public void retryStep(String stepId) {
        StepExecution step = this.steps.get(stepId);
        if (step == null) {
            throw new IllegalArgumentException("Step not found: " + stepId);
        }

        if (!step.canRetry()) {
            throw new IllegalStateException("Step cannot be retried: " + stepId);
        }

        step.retry();
    }

    /**
     * Handle workflow failure
     */
    public void fail(WorkflowError error) {
        this.status = WorkflowStatus.FAILED;
        this.errors.add(error);
        this.completedAt = Instant.now();
        this.executionDurationMs = calculateDuration();

        addDomainEvent(WorkflowFailedEvent.builder()
            .workflowInstanceId(this.id)
            .error(error)
            .failedAt(this.completedAt)
            .build());
    }

    /**
     * Complete workflow successfully
     */
    public void complete() {
        if (this.status != WorkflowStatus.EXECUTING) {
            throw new IllegalStateException("Cannot complete workflow that is not executing");
        }

        this.status = WorkflowStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.executionDurationMs = calculateDuration();

        addDomainEvent(WorkflowCompletedEvent.builder()
            .workflowInstanceId(this.id)
            .completedAt(this.completedAt)
            .executionDurationMs(this.executionDurationMs)
            .build());
    }

    /**
     * Compensate workflow (rollback) - Enhanced saga pattern
     */
    public void compensate() {
        if (this.status != WorkflowStatus.FAILED && this.status != WorkflowStatus.COMPENSATING) {
            throw new IllegalStateException("Can only compensate failed workflows");
        }

        this.status = WorkflowStatus.COMPENSATING;

        // Get steps to compensate (in reverse order)
        List<String> stepsToCompensate = new ArrayList<>(this.executedSteps);
        Collections.reverse(stepsToCompensate);

        addDomainEvent(WorkflowCompensationStartedEvent.builder()
            .workflowInstanceId(this.id)
            .stepsToCompensate(stepsToCompensate)
            .startedAt(Instant.now())
            .build());
    }

    /**
     * Compensate a specific step
     */
    public void compensateStep(String stepId) {
        if (this.status != WorkflowStatus.COMPENSATING) {
            throw new IllegalStateException("Can only compensate steps during compensation");
        }

        StepExecution step = this.steps.get(stepId);
        if (step == null) {
            throw new IllegalArgumentException("Step not found: " + stepId);
        }

        if (!step.requiresCompensation()) {
            throw new IllegalStateException("Step does not require compensation: " + stepId);
        }

        step.compensate();
    }

    /**
     * Mark step as compensated
     */
    public void markStepCompensated(String stepId) {
        StepExecution step = this.steps.get(stepId);
        if (step == null) {
            throw new IllegalArgumentException("Step not found: " + stepId);
        }

        step.markCompensated();

        if (this.compensatedSteps == null) {
            this.compensatedSteps = new ArrayList<>();
        }
        this.compensatedSteps.add(stepId);
    }

    /**
     * Complete compensation successfully
     */
    public void completeCompensation() {
        if (this.status != WorkflowStatus.COMPENSATING) {
            throw new IllegalStateException("Not in compensating status");
        }

        this.status = WorkflowStatus.COMPENSATED;
        this.completedAt = Instant.now();

        addDomainEvent(WorkflowCompensationCompletedEvent.builder()
            .workflowInstanceId(this.id)
            .compensatedSteps(new ArrayList<>(this.compensatedSteps))
            .successful(true)
            .completedAt(this.completedAt)
            .build());
    }

    /**
     * Handle compensation failure
     */
    public void failCompensation(String errorMessage) {
        this.status = WorkflowStatus.COMPENSATED; // Mark as compensated even if partial failure

        addDomainEvent(WorkflowCompensationCompletedEvent.builder()
            .workflowInstanceId(this.id)
            .compensatedSteps(new ArrayList<>(this.compensatedSteps))
            .successful(false)
            .errorMessage(errorMessage)
            .completedAt(Instant.now())
            .build());
    }

    /**
     * Retry workflow execution
     */
    public void retry() {
        if (this.retryCount >= this.maxRetries) {
            throw new IllegalStateException("Maximum retries exceeded");
        }

        this.retryCount++;
        this.status = WorkflowStatus.EXECUTING;
        this.errors.clear();

        addDomainEvent(WorkflowRetryEvent.builder()
            .workflowInstanceId(this.id)
            .retryCount(this.retryCount)
            .retriedAt(Instant.now())
            .build());
    }

    /**
     * Pause workflow execution
     */
    public void pause() {
        if (this.status != WorkflowStatus.EXECUTING) {
            throw new IllegalStateException("Can only pause executing workflows");
        }

        this.status = WorkflowStatus.PAUSED;

        addDomainEvent(WorkflowPausedEvent.builder()
            .workflowInstanceId(this.id)
            .pausedAt(Instant.now())
            .currentStepId(this.currentStepId)
            .build());
    }

    /**
     * Resume workflow execution
     */
    public void resume() {
        if (this.status != WorkflowStatus.PAUSED) {
            throw new IllegalStateException("Can only resume paused workflows");
        }

        this.status = WorkflowStatus.EXECUTING;

        addDomainEvent(WorkflowResumedEvent.builder()
            .workflowInstanceId(this.id)
            .resumedAt(Instant.now())
            .resumeFromStepId(this.currentStepId)
            .build());
    }

    /**
     * Cancel workflow execution
     */
    public void cancel(String reason) {
        if (this.status == WorkflowStatus.COMPLETED || this.status == WorkflowStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel completed or already cancelled workflows");
        }

        this.status = WorkflowStatus.CANCELLED;
        this.completedAt = Instant.now();
        this.executionDurationMs = calculateDuration();

        addDomainEvent(WorkflowCancelledEvent.builder()
            .workflowInstanceId(this.id)
            .reason(reason)
            .cancelledAt(this.completedAt)
            .build());
    }

    /**
     * Update execution context
     */
    public void updateContext(String key, Object value) {
        if (this.executionContext == null) {
            this.executionContext = new ExecutionContext();
        }
        this.executionContext.set(key, value);
    }

    /**
     * Check if workflow can transition to waveless processing
     */
    public boolean canTransitionToWaveless() {
        return this.workflowType != null
            && this.workflowType.supportsWaveless()
            && this.status == WorkflowStatus.EXECUTING
            && this.priority == WorkflowPriority.HIGH;
    }

    /**
     * Transition to waveless processing
     */
    public void transitionToWaveless(int batchSize, long processingIntervalMs) {
        if (!canTransitionToWaveless()) {
            throw new IllegalStateException("Workflow cannot transition to waveless processing");
        }

        updateContext("wavelessEnabled", true);
        updateContext("batchSize", batchSize);
        updateContext("processingIntervalMs", processingIntervalMs);

        addDomainEvent(WavelessProcessingEnabledEvent.builder()
            .workflowInstanceId(this.id)
            .batchSize(batchSize)
            .processingIntervalMs(processingIntervalMs)
            .enabledAt(Instant.now())
            .build());
    }

    /**
     * Calculate system load for this workflow
     */
    public SystemLoad calculateSystemLoad() {
        int activeSteps = (int) steps.values().stream()
            .filter(step -> step.getStatus() == StepStatus.EXECUTING)
            .count();

        return SystemLoad.builder()
            .id("load-" + this.id)
            .workflowInstanceId(this.id)
            .activeSteps(activeSteps)
            .totalSteps(steps.size())
            .utilizationPercentage(calculateUtilization())
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Check if workflow has timed out
     */
    public boolean hasTimedOut(long timeoutMs) {
        if (this.startedAt == null || this.status.isTerminal()) {
            return false;
        }

        long elapsedMs = Instant.now().toEpochMilli() - this.startedAt.toEpochMilli();
        return elapsedMs > timeoutMs;
    }

    /**
     * Get steps that need compensation
     */
    public List<StepExecution> getStepsRequiringCompensation() {
        if (this.executedSteps == null || this.executedSteps.isEmpty()) {
            return List.of();
        }

        List<StepExecution> stepsToCompensate = new ArrayList<>();
        // Reverse order for compensation
        for (int i = executedSteps.size() - 1; i >= 0; i--) {
            String stepId = executedSteps.get(i);
            StepExecution step = steps.get(stepId);
            if (step != null && step.requiresCompensation()) {
                stepsToCompensate.add(step);
            }
        }
        return stepsToCompensate;
    }

    /**
     * Check if all steps are completed
     */
    public boolean allStepsCompleted() {
        if (this.steps == null || this.steps.isEmpty()) {
            return false;
        }

        return this.steps.values().stream()
            .allMatch(step -> step.getStatus() == StepStatus.COMPLETED
                || step.getStatus() == StepStatus.SKIPPED);
    }

    /**
     * Get progress percentage
     */
    public double getProgressPercentage() {
        if (this.steps == null || this.steps.isEmpty()) {
            return 0.0;
        }

        long completedSteps = this.steps.values().stream()
            .filter(step -> step.getStatus() == StepStatus.COMPLETED
                || step.getStatus() == StepStatus.SKIPPED)
            .count();

        return ((double) completedSteps / this.steps.size()) * 100;
    }

    /**
     * Check if workflow is active
     */
    public boolean isActive() {
        return this.status != null && this.status.isActive();
    }

    /**
     * Check if workflow is terminal
     */
    public boolean isTerminal() {
        return this.status != null && this.status.isTerminal();
    }

    // Private helper methods

    private void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    private String getNextStepId(String currentStepId) {
        // Logic to determine next step based on workflow definition
        // This would typically involve looking up the workflow definition
        // and determining the next step based on conditions
        return null; // Placeholder
    }

    private Long calculateDuration() {
        if (startedAt != null && completedAt != null) {
            return completedAt.toEpochMilli() - startedAt.toEpochMilli();
        }
        return 0L;
    }

    private Double calculateUtilization() {
        if (steps == null || steps.isEmpty()) {
            return 0.0;
        }

        long executingSteps = steps.values().stream()
            .filter(step -> step.getStatus() == StepStatus.EXECUTING)
            .count();

        return (double) executingSteps / steps.size() * 100;
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    public List<DomainEvent> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }
}