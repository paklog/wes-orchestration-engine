package com.paklog.wes.orchestration.infrastructure.web.mapper;

import com.paklog.wes.orchestration.application.command.StartWorkflowCommand;
import com.paklog.wes.orchestration.domain.aggregate.WorkflowInstance;
import com.paklog.wes.orchestration.domain.entity.StepExecution;
import com.paklog.wes.orchestration.infrastructure.web.dto.StartWorkflowRequest;
import com.paklog.wes.orchestration.infrastructure.web.dto.StepExecutionResponse;
import com.paklog.wes.orchestration.infrastructure.web.dto.WorkflowResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between domain objects and DTOs
 */
@Component
public class WorkflowDtoMapper {

    /**
     * Convert StartWorkflowRequest to StartWorkflowCommand
     */
    public StartWorkflowCommand toCommand(StartWorkflowRequest request) {
        return StartWorkflowCommand.builder()
            .workflowDefinitionId(request.getWorkflowDefinitionId())
            .workflowName(request.getWorkflowName())
            .workflowType(request.getWorkflowType())
            .priority(request.getPriority())
            .triggeredBy(request.getTriggeredBy())
            .correlationId(request.getCorrelationId())
            .inputParameters(request.getInputParameters())
            .enableWaveless(request.getEnableWaveless() != null ? request.getEnableWaveless() : false)
            .maxRetries(request.getMaxRetries() != null ? request.getMaxRetries() : 3)
            .timeoutMs(request.getTimeoutMs())
            .build();
    }

    /**
     * Convert WorkflowInstance to WorkflowResponse
     */
    public WorkflowResponse toDto(WorkflowInstance workflow) {
        if (workflow == null) {
            return null;
        }

        List<StepExecutionResponse> stepResponses = workflow.getSteps() != null
            ? workflow.getSteps().values().stream()
                .map(this::toStepDto)
                .collect(Collectors.toList())
            : Collections.emptyList();

        return WorkflowResponse.builder()
            .id(workflow.getId())
            .workflowName(workflow.getWorkflowName())
            .workflowType(workflow.getWorkflowType() != null ? workflow.getWorkflowType().name() : null)
            .status(workflow.getStatus() != null ? workflow.getStatus().name() : null)
            .priority(workflow.getPriority() != null ? workflow.getPriority().name() : null)
            .correlationId(workflow.getCorrelationId())
            .inputParameters(workflow.getInputParameters())
            .outputParameters(workflow.getOutputParameters())
            .steps(stepResponses)
            .progressPercentage(workflow.getProgressPercentage())
            .retryCount(workflow.getRetryCount())
            .maxRetries(workflow.getMaxRetries())
            .startedAt(workflow.getStartedAt())
            .completedAt(workflow.getCompletedAt())
            .executionDurationMs(workflow.getExecutionDurationMs())
            .createdAt(workflow.getCreatedAt())
            .updatedAt(workflow.getUpdatedAt())
            .build();
    }

    /**
     * Convert StepExecution to StepExecutionResponse
     */
    public StepExecutionResponse toStepDto(StepExecution step) {
        if (step == null) {
            return null;
        }

        return StepExecutionResponse.builder()
            .stepId(step.getStepId())
            .stepName(step.getStepName())
            .status(step.getStatus() != null ? step.getStatus().name() : null)
            .serviceName(step.getServiceName())
            .operation(step.getOperation())
            .outputData(step.getOutputData())
            .retryCount(step.getRetryCount())
            .startedAt(step.getStartedAt())
            .completedAt(step.getCompletedAt())
            .executionDurationMs(step.getExecutionDurationMs())
            .errorMessage(step.getError() != null ? step.getError().getErrorMessage() : null)
            .build();
    }
}
