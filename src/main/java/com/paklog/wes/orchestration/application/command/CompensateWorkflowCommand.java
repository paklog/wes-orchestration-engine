package com.paklog.wes.orchestration.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Command to compensate (rollback) a workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensateWorkflowCommand {

    @NotBlank(message = "Workflow ID is required")
    private String workflowId;

    @NotBlank(message = "Compensation reason is required")
    private String compensationReason;
}
