package com.paklog.wes.orchestration.domain.event;

import com.paklog.wes.orchestration.domain.valueobject.WorkflowError;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Event emitted when a workflow step fails
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowStepFailedEvent extends DomainEvent {

    private String workflowInstanceId;
    private String stepId;
    private String stepName;
    private WorkflowError error;
    private Boolean willRetry;
    private Integer retryCount;
    private Instant failedAt;

    public WorkflowStepFailedEvent(String workflowInstanceId, String stepId,
                                   String stepName, WorkflowError error,
                                   Boolean willRetry, Integer retryCount) {
        super("WorkflowStepFailed", workflowInstanceId);
        this.workflowInstanceId = workflowInstanceId;
        this.stepId = stepId;
        this.stepName = stepName;
        this.error = error;
        this.willRetry = willRetry;
        this.retryCount = retryCount;
        this.failedAt = Instant.now();
    }
}
