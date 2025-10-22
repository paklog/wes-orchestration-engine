package com.paklog.wes.orchestration.domain.event;

import com.paklog.wes.orchestration.domain.valueobject.WorkflowError;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Event emitted when a workflow fails
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowFailedEvent extends DomainEvent {

    private String workflowInstanceId;
    private WorkflowError error;
    private String failedStepId;
    private Instant failedAt;
    private Boolean compensationRequired;

    public WorkflowFailedEvent(String workflowInstanceId, WorkflowError error) {
        super("WorkflowFailed", workflowInstanceId);
        this.workflowInstanceId = workflowInstanceId;
        this.error = error;
        this.failedAt = Instant.now();
    }
}
