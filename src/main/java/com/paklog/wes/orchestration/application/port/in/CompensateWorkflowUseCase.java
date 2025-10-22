package com.paklog.wes.orchestration.application.port.in;

import com.paklog.wes.orchestration.application.command.CompensateWorkflowCommand;

/**
 * Use case port for workflow compensation (saga rollback)
 */
public interface CompensateWorkflowUseCase {

    /**
     * Compensate (rollback) a workflow using saga pattern
     *
     * @param command The compensation command
     */
    void compensateWorkflow(CompensateWorkflowCommand command);
}
