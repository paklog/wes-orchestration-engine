package com.paklog.wes.orchestration.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Command to retry a failed workflow or specific step
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryWorkflowCommand {

    @NotBlank(message = "Workflow ID is required")
    private String workflowId;

    /**
     * Optional step ID to retry. If null, retry all failed steps.
     */
    private String stepId;
}
