package com.paklog.wes.orchestration.infrastructure.web.controller;

import com.paklog.wes.orchestration.application.service.OrchestrationApplicationService;
import com.paklog.wes.orchestration.application.service.WavelessOrchestrationService;
import com.paklog.wes.orchestration.domain.valueobject.WorkflowStatus;
import com.paklog.wes.orchestration.infrastructure.web.dto.SystemHealthResponse;
import com.paklog.wes.orchestration.infrastructure.web.dto.SystemMetricsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orchestration")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Health & Metrics", description = "Orchestration engine health and metrics")
public class HealthController {

    private final OrchestrationApplicationService orchestrationService;
    private final WavelessOrchestrationService wavelessService;

    @GetMapping("/health")
    @Operation(summary = "Get orchestration engine health")
    public ResponseEntity<SystemHealthResponse> getHealth() {
        log.debug("Getting system health");

        long activeWorkflows = orchestrationService.getWorkflowsByStatus(WorkflowStatus.EXECUTING).size();
        long pendingWorkflows = orchestrationService.getWorkflowsByStatus(WorkflowStatus.PENDING).size();
        long completedWorkflows = orchestrationService.getWorkflowsByStatus(WorkflowStatus.COMPLETED).size();
        long failedWorkflows = orchestrationService.getWorkflowsByStatus(WorkflowStatus.FAILED).size();

        SystemHealthResponse health = SystemHealthResponse.builder()
            .status("UP")
            .version("1.0.0")
            .activeWorkflows(activeWorkflows)
            .pendingWorkflows(pendingWorkflows)
            .completedWorkflows(completedWorkflows)
            .failedWorkflows(failedWorkflows)
            .systemUtilization(calculateUtilization(activeWorkflows))
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.ok(health);
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get system metrics")
    public ResponseEntity<SystemMetricsResponse> getMetrics() {
        log.debug("Getting system metrics");

        Map<String, Object> wavelessMetrics = wavelessService.getWavelessMetrics();

        SystemMetricsResponse metrics = SystemMetricsResponse.builder()
            .totalWorkflows((Long) wavelessMetrics.getOrDefault("totalWorkflows", 0L))
            .activeWorkflows((Long) wavelessMetrics.getOrDefault("activeWorkflows", 0L))
            .highPriorityWorkflows((Long) wavelessMetrics.getOrDefault("highPriority", 0L))
            .normalPriorityWorkflows((Long) wavelessMetrics.getOrDefault("normalPriority", 0L))
            .lowPriorityWorkflows((Long) wavelessMetrics.getOrDefault("lowPriority", 0L))
            .averageExecutionTimeMs(0.0)
            .successRate(95.5)
            .systemLoad((Double) wavelessMetrics.getOrDefault("systemLoad", 0.0))
            .queueDepth((Integer) wavelessMetrics.getOrDefault("queueDepth", 0))
            .additionalMetrics(wavelessMetrics)
            .build();

        return ResponseEntity.ok(metrics);
    }

    private Double calculateUtilization(long activeWorkflows) {
        // Simple calculation - in production this would be more sophisticated
        return Math.min((activeWorkflows / 100.0) * 100, 100.0);
    }
}
