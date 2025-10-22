package com.paklog.wes.orchestration.application.port.in;

import com.paklog.wes.orchestration.application.command.*;
import com.paklog.wes.orchestration.domain.aggregate.WorkflowInstance;

/**
 * Input port for orchestration use cases
 *
 * Defines the interface for all orchestration operations that can be
 * triggered from outside the application (REST API, messaging, etc.)
 */
public interface OrchestrationUseCase {

    /**
     * Start a new workflow instance
     */
    WorkflowInstance startWorkflow(StartWorkflowCommand command);

    /**
     * Execute a step in a workflow
     */
    void executeStep(ExecuteStepCommand command);

    /**
     * Pause a running workflow
     */
    void pauseWorkflow(PauseWorkflowCommand command);

    /**
     * Resume a paused workflow
     */
    void resumeWorkflow(ResumeWorkflowCommand command);

    /**
     * Cancel a workflow
     */
    void cancelWorkflow(CancelWorkflowCommand command);

    /**
     * Retry a failed workflow
     */
    void retryWorkflow(RetryWorkflowCommand command);

    /**
     * Complete a workflow
     */
    void completeWorkflow(CompleteWorkflowCommand command);

    /**
     * Compensate (rollback) a failed workflow
     */
    void compensateWorkflow(CompensateWorkflowCommand command);

    /**
     * Transition workflow to waveless processing
     */
    void transitionToWaveless(TransitionToWavelessCommand command);

    /**
     * Rebalance system load across workflows
     */
    void rebalanceSystemLoad(RebalanceLoadCommand command);
}