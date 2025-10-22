package com.paklog.wes.orchestration.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Event emitted when a workflow is cancelled
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowCancelledEvent extends DomainEvent {

    private String workflowInstanceId;
    private String reason;
    private Instant cancelledAt;
    private String cancelledBy;

    public WorkflowCancelledEvent(String workflowInstanceId, String reason) {
        super("WorkflowCancelled", workflowInstanceId);
        this.workflowInstanceId = workflowInstanceId;
        this.reason = reason;
        this.cancelledAt = Instant.now();
    }
}
