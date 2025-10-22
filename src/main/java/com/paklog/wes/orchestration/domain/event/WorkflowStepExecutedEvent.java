package com.paklog.wes.orchestration.domain.event;

import com.paklog.wes.orchestration.domain.valueobject.StepResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Event emitted when a workflow step is successfully executed
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowStepExecutedEvent extends DomainEvent {

    private String workflowInstanceId;
    private String stepId;
    private String stepName;
    private StepResult result;
    private Instant executedAt;

    public WorkflowStepExecutedEvent(String workflowInstanceId, String stepId,
                                     String stepName, StepResult result) {
        super("WorkflowStepExecuted", workflowInstanceId);
        this.workflowInstanceId = workflowInstanceId;
        this.stepId = stepId;
        this.stepName = stepName;
        this.result = result;
        this.executedAt = Instant.now();
    }
}
