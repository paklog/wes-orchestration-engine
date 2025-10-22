package com.paklog.wes.orchestration.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Event emitted when a workflow is resumed
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowResumedEvent extends DomainEvent {

    private String workflowInstanceId;
    private Instant resumedAt;
    private String resumeFromStepId;
    private String resumedBy;

    public WorkflowResumedEvent(String workflowInstanceId, String resumeFromStepId) {
        super("WorkflowResumed", workflowInstanceId);
        this.workflowInstanceId = workflowInstanceId;
        this.resumeFromStepId = resumeFromStepId;
        this.resumedAt = Instant.now();
    }
}
