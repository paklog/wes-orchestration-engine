package com.paklog.wes.orchestration.application.command;

import com.paklog.wes.orchestration.domain.valueobject.WorkflowPriority;
import com.paklog.wes.orchestration.domain.valueobject.WorkflowType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Command to start a new workflow instance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartWorkflowCommand {

    @NotBlank(message = "Workflow definition ID is required")
    private String workflowDefinitionId;

    @NotBlank(message = "Workflow name is required")
    private String workflowName;

    @NotNull(message = "Workflow type is required")
    private WorkflowType workflowType;

    @Builder.Default
    private WorkflowPriority priority = WorkflowPriority.NORMAL;

    @NotBlank(message = "Triggered by is required")
    private String triggeredBy;

    private String correlationId;

    private Map<String, Object> inputParameters;

    private Map<String, String> headers;

    @Builder.Default
    private boolean enableWaveless = false;

    @Builder.Default
    private Integer maxRetries = 3;

    private Long timeoutMs;

    private String callbackUrl;

    private Map<String, String> metadata;
}