package com.paklog.wes.orchestration.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Command to execute a workflow step
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteStepCommand {

    @NotBlank(message = "Workflow ID is required")
    private String workflowId;

    @NotBlank(message = "Step ID is required")
    private String stepId;

    @NotNull(message = "Input data is required")
    private Map<String, Object> inputData;
}
