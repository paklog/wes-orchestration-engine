package com.paklog.wes.orchestration.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepExecutionResponse {

    private String stepId;
    private String stepName;
    private String status;
    private String serviceName;
    private String operation;
    private Map<String, Object> outputData;
    private Integer retryCount;
    private Instant startedAt;
    private Instant completedAt;
    private Long executionDurationMs;
    private String errorMessage;
}
