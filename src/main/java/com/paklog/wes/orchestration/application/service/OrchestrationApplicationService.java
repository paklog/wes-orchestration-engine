package com.paklog.wes.orchestration.application.service;

import com.paklog.wes.orchestration.application.command.*;
import com.paklog.wes.orchestration.application.port.in.CompensateWorkflowUseCase;
import com.paklog.wes.orchestration.application.port.in.ManageWorkflowUseCase;
import com.paklog.wes.orchestration.application.port.in.OrchestrationUseCase;
import com.paklog.wes.orchestration.application.port.out.PublishEventPort;
import com.paklog.wes.orchestration.domain.aggregate.WorkflowInstance;
import com.paklog.wes.orchestration.domain.entity.StepExecution;
import com.paklog.wes.orchestration.domain.event.DomainEvent;
import com.paklog.wes.orchestration.domain.repository.WorkflowInstanceRepository;
import com.paklog.wes.orchestration.domain.service.SagaCoordinatorService;
import com.paklog.wes.orchestration.domain.service.WorkflowExecutionService;
import com.paklog.wes.orchestration.domain.valueobject.StepResult;
import com.paklog.wes.orchestration.domain.valueobject.WorkflowError;
import com.paklog.wes.orchestration.domain.valueobject.WorkflowStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Main Orchestration Application Service
 * Implements use cases for workflow orchestration
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestrationApplicationService implements OrchestrationUseCase, ManageWorkflowUseCase, CompensateWorkflowUseCase {

    private final WorkflowInstanceRepository workflowRepository;
    private final SagaCoordinatorService sagaCoordinatorService;
    private final WorkflowExecutionService workflowExecutionService;
    private final PublishEventPort publishEventPort;

    @Override
    @Transactional
    public String startWorkflow(StartWorkflowCommand command) {
        log.info("Starting workflow: {} of type {}", command.getWorkflowName(), command.getWorkflowType());

        // Create workflow instance
        WorkflowInstance workflow = WorkflowInstance.builder()
            .id(UUID.randomUUID().toString())
            .workflowDefinitionId(command.getWorkflowDefinitionId())
            .workflowName(command.getWorkflowName())
            .workflowType(command.getWorkflowType())
            .status(WorkflowStatus.PENDING)
            .priority(command.getPriority())
            .triggeredBy(command.getTriggeredBy())
            .correlationId(command.getCorrelationId() != null ? command.getCorrelationId() : UUID.randomUUID().toString())
            .inputParameters(command.getInputParameters())
            .maxRetries(command.getMaxRetries())
            .retryCount(0)
            .errors(List.of())
            .build();

        // Start saga for workflow
        sagaCoordinatorService.startSaga(workflow);

        // Enable waveless if requested
        if (command.isEnableWaveless() && workflow.canTransitionToWaveless()) {
            workflow.transitionToWaveless(10, 1000L);
        }

        // Save workflow
        WorkflowInstance savedWorkflow = workflowRepository.save(workflow);

        // Publish domain events
        publishDomainEvents(savedWorkflow);

        log.info("Workflow started successfully: {}", savedWorkflow.getId());
        return savedWorkflow.getId();
    }

    @Override
    @Transactional
    public void pauseWorkflow(PauseWorkflowCommand command) {
        log.info("Pausing workflow: {}", command.getWorkflowId());

        WorkflowInstance workflow = getWorkflowById(command.getWorkflowId());

        workflowExecutionService.pauseExecution(workflow);

        WorkflowInstance savedWorkflow = workflowRepository.save(workflow);
        publishDomainEvents(savedWorkflow);

        log.info("Workflow paused: {}", command.getWorkflowId());
    }

    @Override
    @Transactional
    public void resumeWorkflow(ResumeWorkflowCommand command) {
        log.info("Resuming workflow: {}", command.getWorkflowId());

        WorkflowInstance workflow = getWorkflowById(command.getWorkflowId());

        workflowExecutionService.resumeExecution(workflow);

        WorkflowInstance savedWorkflow = workflowRepository.save(workflow);
        publishDomainEvents(savedWorkflow);

        log.info("Workflow resumed: {}", command.getWorkflowId());
    }

    @Override
    @Transactional
    public void cancelWorkflow(CancelWorkflowCommand command) {
        log.info("Cancelling workflow: {} - Reason: {}", command.getWorkflowId(), command.getReason());

        WorkflowInstance workflow = getWorkflowById(command.getWorkflowId());

        workflowExecutionService.cancelExecution(workflow, command.getReason());

        WorkflowInstance savedWorkflow = workflowRepository.save(workflow);
        publishDomainEvents(savedWorkflow);

        log.info("Workflow cancelled: {}", command.getWorkflowId());
    }

    @Override
    @Transactional
    public void compensateWorkflow(CompensateWorkflowCommand command) {
        log.info("Compensating workflow: {} - Reason: {}", command.getWorkflowId(), command.getCompensationReason());

        WorkflowInstance workflow = getWorkflowById(command.getWorkflowId());

        // Create error for compensation reason
        WorkflowError compensationError = WorkflowError.create(
            null,
            "orchestration-service",
            "MANUAL_COMPENSATION",
            command.getCompensationReason(),
            WorkflowError.ErrorType.BUSINESS_RULE_VIOLATION.name(),
            false
        );

        // Execute backward recovery (compensation)
        sagaCoordinatorService.executeBackwardRecovery(workflow, compensationError);

        // Compensate each step that requires compensation
        List<StepExecution> stepsToCompensate = workflow.getStepsRequiringCompensation();
        for (StepExecution step : stepsToCompensate) {
            workflow.compensateStep(step.getStepId());
            // In real implementation, this would call the service to execute compensation
            workflow.markStepCompensated(step.getStepId());
        }

        // Complete compensation
        workflow.completeCompensation();

        WorkflowInstance savedWorkflow = workflowRepository.save(workflow);
        publishDomainEvents(savedWorkflow);

        log.info("Workflow compensation completed: {}", command.getWorkflowId());
    }

    @Override
    @Transactional
    public void retryWorkflow(RetryWorkflowCommand command) {
        log.info("Retrying workflow: {} - Step: {}", command.getWorkflowId(), command.getStepId());

        WorkflowInstance workflow = getWorkflowById(command.getWorkflowId());

        if (command.getStepId() != null) {
            // Retry specific step
            workflow.retryStep(command.getStepId());
            log.info("Retrying step {} for workflow {}", command.getStepId(), command.getWorkflowId());
        } else {
            // Retry entire workflow
            workflow.retry();
            log.info("Retrying entire workflow: {}", command.getWorkflowId());
        }

        WorkflowInstance savedWorkflow = workflowRepository.save(workflow);
        publishDomainEvents(savedWorkflow);

        log.info("Workflow retry initiated: {}", command.getWorkflowId());
    }

    @Override
    @Transactional
    public void executeStep(ExecuteStepCommand command) {
        log.info("Executing step {} for workflow {}", command.getStepId(), command.getWorkflowId());

        WorkflowInstance workflow = getWorkflowById(command.getWorkflowId());

        // Create step result (in real implementation, this would come from service call)
        StepResult result = StepResult.success(
            command.getStepId(),
            command.getInputData(),
            0L
        );

        // Execute step through workflow execution service
        workflowExecutionService.executeStep(workflow, command.getStepId(), result);

        WorkflowInstance savedWorkflow = workflowRepository.save(workflow);
        publishDomainEvents(savedWorkflow);

        log.info("Step executed successfully: {} for workflow {}", command.getStepId(), command.getWorkflowId());
    }

    /**
     * Get workflow status
     */
    public WorkflowInstance getWorkflowStatus(String workflowId) {
        log.debug("Getting workflow status: {}", workflowId);
        return getWorkflowById(workflowId);
    }

    /**
     * Get all active workflows
     */
    public List<WorkflowInstance> getActiveWorkflows() {
        log.debug("Getting all active workflows");
        return workflowRepository.findActiveWorkflows();
    }

    /**
     * Get workflows by status
     */
    public List<WorkflowInstance> getWorkflowsByStatus(WorkflowStatus status) {
        log.debug("Getting workflows by status: {}", status);
        return workflowRepository.findByStatus(status);
    }

    // Private helper methods

    private WorkflowInstance getWorkflowById(String workflowId) {
        return workflowRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));
    }

    private void publishDomainEvents(WorkflowInstance workflow) {
        List<DomainEvent> events = workflow.getDomainEvents();
        for (DomainEvent event : events) {
            try {
                publishEventPort.publishEvent(event);
                log.debug("Published event: {} for workflow {}", event.getEventType(), workflow.getId());
            } catch (Exception e) {
                log.error("Failed to publish event: {} for workflow {}", event.getEventType(), workflow.getId(), e);
            }
        }
        workflow.clearDomainEvents();
    }
}
