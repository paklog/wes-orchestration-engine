package com.paklog.wes.orchestration.infrastructure.persistence.mongodb.document;

import com.paklog.wes.orchestration.domain.valueobject.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * MongoDB document for WorkflowInstance aggregate
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workflow_instances")
public class WorkflowInstanceDocument {

    @Id
    private String id;

    private String workflowDefinitionId;
    private String workflowName;

    @Indexed
    private String workflowType;

    @Indexed
    private String status;

    private ExecutionContext executionContext;
    private Map<String, StepExecutionDocument> steps;
    private List<String> executedSteps;
    private List<String> compensatedSteps;
    private String currentStepId;

    @Indexed
    private String priority;

    private String triggeredBy;

    @Indexed
    private String correlationId;

    private Map<String, Object> inputParameters;
    private Map<String, Object> outputParameters;
    private List<WorkflowError> errors;
    private Integer retryCount;
    private Integer maxRetries;

    @Indexed
    private Instant startedAt;

    private Instant completedAt;
    private Long executionDurationMs;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
