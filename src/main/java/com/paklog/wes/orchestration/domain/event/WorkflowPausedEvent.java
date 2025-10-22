package com.paklog.wes.orchestration.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Event emitted when a workflow is paused
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowPausedEvent extends DomainEvent {

    private String workflowInstanceId;
    private Instant pausedAt;
    private String currentStepId;
    private String reason;
    private String pausedBy;

    public WorkflowPausedEvent(String workflowInstanceId, String currentStepId) {
        super("WorkflowPaused", workflowInstanceId);
        this.workflowInstanceId = workflowInstanceId;
        this.currentStepId = currentStepId;
        this.pausedAt = Instant.now();
    }
}
