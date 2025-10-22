package com.paklog.wes.orchestration.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthResponse {

    private String status;
    private String version;
    private Long activeWorkflows;
    private Long pendingWorkflows;
    private Long completedWorkflows;
    private Long failedWorkflows;
    private Double systemUtilization;
    private Instant timestamp;
}
