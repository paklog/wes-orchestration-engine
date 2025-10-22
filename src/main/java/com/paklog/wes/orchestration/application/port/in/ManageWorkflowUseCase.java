package com.paklog.wes.orchestration.application.port.in;

import com.paklog.wes.orchestration.application.command.*;

/**
 * Use case port for managing workflow lifecycle operations
 */
public interface ManageWorkflowUseCase {

    /**
     * Pause a workflow execution
     *
     * @param command The pause command
     */
    void pauseWorkflow(PauseWorkflowCommand command);

    /**
     * Resume a paused workflow
     *
     * @param command The resume command
     */
    void resumeWorkflow(ResumeWorkflowCommand command);

    /**
     * Cancel a workflow execution
     *
     * @param command The cancel command
     */
    void cancelWorkflow(CancelWorkflowCommand command);

    /**
     * Retry a failed workflow or specific step
     *
     * @param command The retry command
     */
    void retryWorkflow(RetryWorkflowCommand command);

    /**
     * Execute a workflow step
     *
     * @param command The execute step command
     */
    void executeStep(ExecuteStepCommand command);
}
