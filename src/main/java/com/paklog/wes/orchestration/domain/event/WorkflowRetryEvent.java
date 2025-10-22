package com.paklog.wes.orchestration.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Event emitted when a workflow is retried
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowRetryEvent extends DomainEvent {

    private String workflowInstanceId;
    private Integer retryCount;
    private String retryReason;
    private Instant retriedAt;

    public WorkflowRetryEvent(String workflowInstanceId, Integer retryCount) {
        super("WorkflowRetry", workflowInstanceId);
        this.workflowInstanceId = workflowInstanceId;
        this.retryCount = retryCount;
        this.retriedAt = Instant.now();
    }
}
