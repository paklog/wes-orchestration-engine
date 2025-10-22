package com.paklog.wes.orchestration.infrastructure.persistence.mongodb.document;

import com.paklog.wes.orchestration.domain.valueobject.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * MongoDB document for StepExecution entity (embedded document)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepExecutionDocument {

    private String stepId;
    private String workflowInstanceId;
    private String stepName;
    private String stepType;
    private String serviceName;
    private String operation;
    private Integer executionOrder;
    private String status;
    private Map<String, Object> inputData;
    private Map<String, Object> outputData;
    private StepResult result;
    private Integer retriesRemaining;
    private Integer retryCount;
    private RetryPolicy retryPolicy;
    private CompensationAction compensationAction;
    private Long timeoutMs;
    private Instant startedAt;
    private Instant completedAt;
    private Instant executedAt;
    private Long executionDurationMs;
    private Boolean compensated;
    private Instant compensatedAt;
    private WorkflowError error;
}
