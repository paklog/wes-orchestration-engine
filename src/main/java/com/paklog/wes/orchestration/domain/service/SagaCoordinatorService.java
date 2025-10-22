package com.paklog.wes.orchestration.domain.service;

import com.paklog.wes.orchestration.domain.aggregate.WorkflowInstance;
import com.paklog.wes.orchestration.domain.entity.StepExecution;
import com.paklog.wes.orchestration.domain.valueobject.CompensationAction;
import com.paklog.wes.orchestration.domain.valueobject.WorkflowError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Saga Coordinator Domain Service
 *
 * Coordinates distributed transactions using the Saga pattern.
 * Manages compensation logic for rollback on failure.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaCoordinatorService {

    /**
     * Start saga for workflow
     */
    public void startSaga(WorkflowInstance workflow) {
        log.info("Starting saga for workflow: {}", workflow.getId());

        // Initialize saga state tracking
        workflow.updateContext("sagaStarted", true);
        workflow.updateContext("sagaTransactionId", generateTransactionId());

        workflow.start();
    }

    /**
     * Execute forward recovery (retry failed steps)
     */
    public boolean executeForwardRecovery(WorkflowInstance workflow, String failedStepId) {
        log.info("Attempting forward recovery for workflow: {}, step: {}",
            workflow.getId(), failedStepId);

        StepExecution failedStep = workflow.getSteps().get(failedStepId);
        if (failedStep == null) {
            log.error("Failed step not found: {}", failedStepId);
            return false;
        }

        // Check if step can be retried
        if (!failedStep.canRetry()) {
            log.warn("Step {} cannot be retried. Initiating backward recovery.", failedStepId);
            return false;
        }

        // Retry the step
        workflow.retryStep(failedStepId);
        log.info("Step {} marked for retry. Retry count: {}", failedStepId, failedStep.getRetryCount());

        return true;
    }

    /**
     * Execute backward recovery (compensate completed steps)
     */
    public void executeBackwardRecovery(WorkflowInstance workflow, WorkflowError error) {
        log.info("Executing backward recovery (compensation) for workflow: {}", workflow.getId());

        // Get steps that need compensation (in reverse order)
        List<StepExecution> stepsToCompensate = workflow.getStepsRequiringCompensation();

        if (stepsToCompensate.isEmpty()) {
            log.info("No steps require compensation for workflow: {}", workflow.getId());
            workflow.completeCompensation();
            return;
        }

        log.info("Compensating {} steps for workflow: {}",
            stepsToCompensate.size(), workflow.getId());

        // Initiate compensation
        workflow.compensate();

        // Track compensation state
        workflow.updateContext("compensationReason", error.getErrorMessage());
        workflow.updateContext("stepsToCompensate", stepsToCompensate.size());
    }

    /**
     * Compensate a single step in the saga
     */
    public CompensationAction getCompensationAction(StepExecution step) {
        if (step.getCompensationAction() == null) {
            log.warn("No compensation action defined for step: {}", step.getStepId());
            return null;
        }

        CompensationAction action = step.getCompensationAction();

        // Validate compensation action
        if (!action.isValid()) {
            log.error("Invalid compensation action for step: {}", step.getStepId());
            return null;
        }

        log.debug("Compensation action retrieved for step {}: {} -> {}",
            step.getStepId(), action.getStrategy(), action.getOperation());

        return action;
    }

    /**
     * Complete saga successfully
     */
    public void completeSaga(WorkflowInstance workflow) {
        log.info("Completing saga for workflow: {}", workflow.getId());

        workflow.updateContext("sagaCompleted", true);
        workflow.updateContext("sagaEndTime", System.currentTimeMillis());

        workflow.complete();
    }

    /**
     * Fail saga and trigger compensation
     */
    public void failSaga(WorkflowInstance workflow, WorkflowError error) {
        log.error("Saga failed for workflow: {}. Error: {}",
            workflow.getId(), error.getErrorMessage());

        workflow.updateContext("sagaFailed", true);
        workflow.updateContext("sagaFailureReason", error.getErrorMessage());

        // Mark workflow as failed
        workflow.fail(error);

        // Determine if compensation is needed
        if (error.requiresCompensation()) {
            executeBackwardRecovery(workflow, error);
        } else {
            log.info("Error is recoverable, skipping compensation");
        }
    }

    /**
     * Check saga state consistency
     */
    public boolean checkSagaConsistency(WorkflowInstance workflow) {
        // Verify all completed steps have compensation actions
        long completedWithoutCompensation = workflow.getSteps().values().stream()
            .filter(step -> step.getStatus().name().equals("COMPLETED"))
            .filter(step -> step.getCompensationAction() == null)
            .count();

        if (completedWithoutCompensation > 0) {
            log.warn("Workflow {} has {} completed steps without compensation actions",
                workflow.getId(), completedWithoutCompensation);
        }

        return completedWithoutCompensation == 0;
    }

    /**
     * Calculate compensation percentage
     */
    public double calculateCompensationProgress(WorkflowInstance workflow) {
        List<String> executedSteps = workflow.getExecutedSteps();
        List<String> compensatedSteps = workflow.getCompensatedSteps();

        if (executedSteps == null || executedSteps.isEmpty()) {
            return 100.0;
        }

        if (compensatedSteps == null) {
            return 0.0;
        }

        return ((double) compensatedSteps.size() / executedSteps.size()) * 100;
    }

    /**
     * Check if saga can proceed to next step
     */
    public boolean canProceedToNextStep(WorkflowInstance workflow, String currentStepId) {
        StepExecution currentStep = workflow.getSteps().get(currentStepId);

        if (currentStep == null) {
            return false;
        }

        // Check if current step is completed
        if (!currentStep.getStatus().name().equals("COMPLETED")) {
            return false;
        }

        // Check if workflow is in valid state
        return workflow.getStatus().name().equals("EXECUTING");
    }

    // Private helper methods

    private String generateTransactionId() {
        return "saga-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 10000);
    }
}
