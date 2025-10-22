package com.paklog.wes.orchestration.domain.service;

import com.paklog.wes.orchestration.domain.aggregate.WorkflowInstance;
import com.paklog.wes.orchestration.domain.entity.StepExecution;
import com.paklog.wes.orchestration.domain.entity.WorkflowDefinition;
import com.paklog.wes.orchestration.domain.valueobject.RetryPolicy;
import com.paklog.wes.orchestration.domain.valueobject.StepResult;
import com.paklog.wes.orchestration.domain.valueobject.WorkflowError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Workflow Execution Domain Service
 *
 * Manages step-by-step execution of workflows with retry logic,
 * timeout management, and error handling.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowExecutionService {

    private final SagaCoordinatorService sagaCoordinator;

    /**
     * Execute workflow step
     */
    public void executeStep(WorkflowInstance workflow, String stepId, StepResult result) {
        log.info("Executing step {} for workflow {}", stepId, workflow.getId());

        try {
            // Validate step can be executed
            validateStepExecution(workflow, stepId);

            // Execute the step
            workflow.executeStep(stepId, result);

            log.info("Step {} executed successfully for workflow {}", stepId, workflow.getId());

            // Check if workflow is complete
            if (workflow.allStepsCompleted()) {
                sagaCoordinator.completeSaga(workflow);
            }

        } catch (Exception e) {
            log.error("Error executing step {} for workflow {}: {}",
                stepId, workflow.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Handle step execution failure with retry logic
     */
    public void handleStepFailure(WorkflowInstance workflow, String stepId,
                                  WorkflowError error) {
        log.warn("Step {} failed for workflow {}: {}",
            stepId, workflow.getId(), error.getErrorMessage());

        workflow.handleStepFailure(stepId, error);

        StepExecution failedStep = workflow.getSteps().get(stepId);

        // Attempt forward recovery (retry)
        if (failedStep.canRetry()) {
            boolean retrySuccessful = sagaCoordinator.executeForwardRecovery(workflow, stepId);

            if (retrySuccessful) {
                long delay = failedStep.calculateRetryDelay();
                log.info("Will retry step {} after {} ms", stepId, delay);
                workflow.updateContext("retryDelay_" + stepId, delay);
                return;
            }
        }

        // If retry not possible or not successful, trigger saga failure
        if (!error.isRecoverable()) {
            log.error("Step {} failed with unrecoverable error. Failing saga.", stepId);
            sagaCoordinator.failSaga(workflow, error);
        }
    }

    /**
     * Execute step with timeout management
     */
    public void executeStepWithTimeout(WorkflowInstance workflow, String stepId,
                                       Long timeoutMs) {
        log.info("Starting step {} execution with timeout {} ms", stepId, timeoutMs);

        workflow.startStepExecution(stepId);

        // Timeout tracking is handled by the step execution entity
        StepExecution step = workflow.getSteps().get(stepId);
        if (step.hasTimedOut()) {
            WorkflowError timeoutError = WorkflowError.create(
                stepId,
                step.getServiceName(),
                "TIMEOUT",
                "Step execution exceeded timeout of " + timeoutMs + " ms",
                WorkflowError.ErrorType.TIMEOUT.name(),
                true
            );
            handleStepFailure(workflow, stepId, timeoutError);
        }
    }

    /**
     * Get next step to execute
     */
    public Optional<String> getNextStep(WorkflowInstance workflow, WorkflowDefinition definition) {
        String currentStepId = workflow.getCurrentStepId();

        Optional<WorkflowDefinition.StepDefinition> nextStepDef =
            definition.getNextStep(currentStepId);

        if (nextStepDef.isEmpty()) {
            log.info("No more steps to execute for workflow {}", workflow.getId());
            return Optional.empty();
        }

        // Check if dependencies are satisfied
        WorkflowDefinition.StepDefinition stepDef = nextStepDef.get();
        boolean dependenciesSatisfied = definition.areDependenciesSatisfied(
            stepDef.getStepId(),
            workflow.getExecutedSteps()
        );

        if (!dependenciesSatisfied) {
            log.warn("Dependencies not satisfied for step {}", stepDef.getStepId());
            return Optional.empty();
        }

        return Optional.of(stepDef.getStepId());
    }

    /**
     * Apply retry policy with exponential backoff
     */
    public long calculateRetryDelay(RetryPolicy policy, int retryAttempt) {
        if (policy == null) {
            policy = RetryPolicy.defaultPolicy();
        }

        long delay = policy.calculateDelay(retryAttempt);
        log.debug("Calculated retry delay for attempt {}: {} ms", retryAttempt, delay);

        return delay;
    }

    /**
     * Validate workflow can continue execution
     */
    public boolean canContinueExecution(WorkflowInstance workflow) {
        // Check workflow status
        if (!workflow.isActive()) {
            log.warn("Workflow {} is not active. Status: {}",
                workflow.getId(), workflow.getStatus());
            return false;
        }

        // Check for timeout
        if (workflow.hasTimedOut(300000L)) { // 5 minutes default
            log.error("Workflow {} has timed out", workflow.getId());
            return false;
        }

        return true;
    }

    /**
     * Pause workflow execution
     */
    public void pauseExecution(WorkflowInstance workflow) {
        log.info("Pausing workflow execution: {}", workflow.getId());
        workflow.pause();
    }

    /**
     * Resume workflow execution
     */
    public void resumeExecution(WorkflowInstance workflow) {
        log.info("Resuming workflow execution: {}", workflow.getId());
        workflow.resume();
    }

    /**
     * Cancel workflow execution
     */
    public void cancelExecution(WorkflowInstance workflow, String reason) {
        log.info("Cancelling workflow execution: {}. Reason: {}", workflow.getId(), reason);
        workflow.cancel(reason);
    }

    /**
     * Calculate execution progress
     */
    public double calculateProgress(WorkflowInstance workflow) {
        return workflow.getProgressPercentage();
    }

    // Private helper methods

    private void validateStepExecution(WorkflowInstance workflow, String stepId) {
        if (!workflow.isActive()) {
            throw new IllegalStateException(
                "Workflow is not active. Status: " + workflow.getStatus()
            );
        }

        StepExecution step = workflow.getSteps().get(stepId);
        if (step == null) {
            throw new IllegalArgumentException("Step not found: " + stepId);
        }

        if (step.isTerminal()) {
            throw new IllegalStateException(
                "Step is already in terminal state: " + step.getStatus()
            );
        }
    }
}
