package com.paklog.wes.orchestration.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;

/**
 * Event emitted when workflow compensation completes
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowCompensationCompletedEvent extends DomainEvent {

    private String workflowInstanceId;
    private List<String> compensatedSteps;
    private Instant completedAt;
    private Boolean successful;
    private String errorMessage;

    public WorkflowCompensationCompletedEvent(String workflowInstanceId, List<String> compensatedSteps,
                                              Boolean successful) {
        super("WorkflowCompensationCompleted", workflowInstanceId);
        this.workflowInstanceId = workflowInstanceId;
        this.compensatedSteps = compensatedSteps;
        this.successful = successful;
        this.completedAt = Instant.now();
    }
}
