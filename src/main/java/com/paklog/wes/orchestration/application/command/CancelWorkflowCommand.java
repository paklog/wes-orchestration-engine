package com.paklog.wes.orchestration.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Command to cancel a workflow execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelWorkflowCommand {

    @NotBlank(message = "Workflow ID is required")
    private String workflowId;

    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}
