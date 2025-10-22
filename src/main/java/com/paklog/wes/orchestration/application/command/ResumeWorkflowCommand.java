package com.paklog.wes.orchestration.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Command to resume a paused workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeWorkflowCommand {

    @NotBlank(message = "Workflow ID is required")
    private String workflowId;
}
