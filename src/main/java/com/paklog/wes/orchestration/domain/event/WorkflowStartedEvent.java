package com.paklog.wes.orchestration.domain.event;

import com.paklog.wes.orchestration.domain.valueobject.WorkflowType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Event emitted when a workflow starts execution
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowStartedEvent extends DomainEvent {

    private String workflowInstanceId;
    private String workflowDefinitionId;
    private WorkflowType workflowType;
    private String correlationId;
    private Instant startedAt;
    private String triggeredBy;

    public WorkflowStartedEvent(String workflowInstanceId, String workflowDefinitionId,
                                WorkflowType workflowType, String correlationId) {
        super("WorkflowStarted", workflowInstanceId);
        this.workflowInstanceId = workflowInstanceId;
        this.workflowDefinitionId = workflowDefinitionId;
        this.workflowType = workflowType;
        this.correlationId = correlationId;
        this.startedAt = Instant.now();
    }
}
