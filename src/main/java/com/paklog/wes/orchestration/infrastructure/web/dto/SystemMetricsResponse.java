package com.paklog.wes.orchestration.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetricsResponse {

    private Long totalWorkflows;
    private Long activeWorkflows;
    private Long highPriorityWorkflows;
    private Long normalPriorityWorkflows;
    private Long lowPriorityWorkflows;
    private Double averageExecutionTimeMs;
    private Double successRate;
    private Double systemLoad;
    private Integer queueDepth;
    private Map<String, Object> additionalMetrics;
}
