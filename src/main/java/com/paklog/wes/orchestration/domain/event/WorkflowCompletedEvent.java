package com.paklog.wes.orchestration.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;

/**
 * Event emitted when a workflow completes successfully
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowCompletedEvent extends DomainEvent {

    private String workflowInstanceId;
    private Instant completedAt;
    private Long executionDurationMs;
    private Integer totalSteps;
    private Map<String, Object> outputParameters;

    public WorkflowCompletedEvent(String workflowInstanceId, Instant completedAt,
                                  Long executionDurationMs) {
        super("WorkflowCompleted", workflowInstanceId);
        this.workflowInstanceId = workflowInstanceId;
        this.completedAt = completedAt;
        this.executionDurationMs = executionDurationMs;
    }
}
