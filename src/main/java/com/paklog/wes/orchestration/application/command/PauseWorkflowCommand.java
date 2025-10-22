package com.paklog.wes.orchestration.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Command to pause a workflow execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PauseWorkflowCommand {

    @NotBlank(message = "Workflow ID is required")
    private String workflowId;

    private String reason;
}
