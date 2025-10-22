package com.paklog.wes.orchestration.domain.aggregate;

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
     * Execute a workflow step
     */
    public void executeStep(String stepId, StepResult result) {
        if (this.status != WorkflowStatus.EXECUTING) {
            throw new IllegalStateException("Cannot execute step when workflow is not in EXECUTING status");
        }

        StepExecution step = this.steps.get(stepId);
        if (step == null) {
            throw new IllegalArgumentException("Step not found: " + stepId);
        }

        step.setStatus(StepStatus.COMPLETED);
        step.setExecutedAt(Instant.now());
        step.setResult(result);

        this.executedSteps.add(stepId);
        this.currentStepId = getNextStepId(stepId);

        addDomainEvent(WorkflowStepExecutedEvent.builder()
            .workflowInstanceId(this.id)
            .stepId(stepId)
            .stepName(step.getStepName())
            .result(result)
            .executedAt(step.getExecutedAt())
            .build());
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
     * Compensate workflow (rollback)
     */
    public void compensate() {
        if (this.status != WorkflowStatus.FAILED && this.status != WorkflowStatus.COMPENSATING) {
            throw new IllegalStateException("Can only compensate failed workflows");
        }

        this.status = WorkflowStatus.COMPENSATING;

        // Execute compensation in reverse order
        Collections.reverse(this.executedSteps);

        addDomainEvent(WorkflowCompensationStartedEvent.builder()
            .workflowInstanceId(this.id)
            .stepsToCompensate(new ArrayList<>(this.executedSteps))
            .startedAt(Instant.now())
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
        return this.workflowType == WorkflowType.ORDER_FULFILLMENT
            && this.status == WorkflowStatus.EXECUTING
            && this.priority == WorkflowPriority.HIGH;
    }

    /**
     * Calculate system load for this workflow
     */
    public SystemLoad calculateSystemLoad() {
        int activeSteps = (int) steps.values().stream()
            .filter(step -> step.getStatus() == StepStatus.EXECUTING)
            .count();

        return SystemLoad.builder()
            .workflowInstanceId(this.id)
            .activeSteps(activeSteps)
            .totalSteps(steps.size())
            .utilizationPercentage(calculateUtilization())
            .build();
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