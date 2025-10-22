package com.paklog.wes.orchestration.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResponse {

    private String id;
    private String workflowName;
    private String workflowType;
    private String status;
    private String priority;
    private String correlationId;
    private Map<String, Object> inputParameters;
    private Map<String, Object> outputParameters;
    private List<StepExecutionResponse> steps;
    private Double progressPercentage;
    private Integer retryCount;
    private Integer maxRetries;
    private Instant startedAt;
    private Instant completedAt;
    private Long executionDurationMs;
    private Instant createdAt;
    private Instant updatedAt;
}
