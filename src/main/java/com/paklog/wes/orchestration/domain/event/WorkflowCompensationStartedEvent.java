package com.paklog.wes.orchestration.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;

/**
 * Event emitted when workflow compensation (rollback) starts
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowCompensationStartedEvent extends DomainEvent {

    private String workflowInstanceId;
    private List<String> stepsToCompensate;
    private Instant startedAt;
    private String reason;

    public WorkflowCompensationStartedEvent(String workflowInstanceId, List<String> stepsToCompensate) {
        super("WorkflowCompensationStarted", workflowInstanceId);
        this.workflowInstanceId = workflowInstanceId;
        this.stepsToCompensate = stepsToCompensate;
        this.startedAt = Instant.now();
    }
}
