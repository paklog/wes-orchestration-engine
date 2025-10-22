package com.paklog.wes.orchestration.infrastructure.web.controller;

import com.paklog.wes.orchestration.application.command.*;
import com.paklog.wes.orchestration.application.service.OrchestrationApplicationService;
import com.paklog.wes.orchestration.domain.aggregate.WorkflowInstance;
import com.paklog.wes.orchestration.infrastructure.web.dto.*;
import com.paklog.wes.orchestration.infrastructure.web.mapper.WorkflowDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for workflow orchestration operations
 */
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Workflow Orchestration", description = "WES orchestration engine API")
public class WorkflowController {

    private final OrchestrationApplicationService orchestrationService;
    private final WorkflowDtoMapper workflowMapper;

    @PostMapping
    @Operation(summary = "Start a new workflow instance")
    public ResponseEntity<String> startWorkflow(@Valid @RequestBody StartWorkflowRequest request) {
        log.info("Starting workflow: {}", request.getWorkflowName());

        StartWorkflowCommand command = workflowMapper.toCommand(request);
        String workflowId = orchestrationService.startWorkflow(command);

        log.info("Workflow started successfully with ID: {}", workflowId);
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workflow status")
    public ResponseEntity<WorkflowResponse> getWorkflow(@PathVariable String id) {
        log.debug("Getting workflow: {}", id);

        WorkflowInstance workflow = orchestrationService.getWorkflowStatus(id);
        WorkflowResponse response = workflowMapper.toDto(workflow);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{workflowId}/steps/execute")
    @Operation(summary = "Execute a step in a workflow")
    public ResponseEntity<Void> executeStep(
            @PathVariable String workflowId,
            @Valid @RequestBody ExecuteStepRequest request) {
        log.info("Executing step {} in workflow {}", request.getStepId(), workflowId);

        ExecuteStepCommand command = ExecuteStepCommand.builder()
            .workflowId(workflowId)
            .stepId(request.getStepId())
            .inputData(request.getInputData())
            .build();

        orchestrationService.executeStep(command);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{workflowId}/pause")
    @Operation(summary = "Pause a running workflow")
    public ResponseEntity<Void> pauseWorkflow(@PathVariable String workflowId) {
        log.info("Pausing workflow: {}", workflowId);

        PauseWorkflowCommand command = PauseWorkflowCommand.builder()
            .workflowId(workflowId)
            .build();

        orchestrationService.pauseWorkflow(command);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{workflowId}/resume")
    @Operation(summary = "Resume a paused workflow")
    public ResponseEntity<Void> resumeWorkflow(@PathVariable String workflowId) {
        log.info("Resuming workflow: {}", workflowId);

        ResumeWorkflowCommand command = ResumeWorkflowCommand.builder()
            .workflowId(workflowId)
            .build();

        orchestrationService.resumeWorkflow(command);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{workflowId}/cancel")
    @Operation(summary = "Cancel a workflow")
    public ResponseEntity<Void> cancelWorkflow(
            @PathVariable String workflowId,
            @RequestParam(required = false) String reason) {
        log.info("Cancelling workflow: {} with reason: {}", workflowId, reason);

        CancelWorkflowCommand command = CancelWorkflowCommand.builder()
            .workflowId(workflowId)
            .reason(reason != null ? reason : "User requested cancellation")
            .build();

        orchestrationService.cancelWorkflow(command);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{workflowId}/retry")
    @Operation(summary = "Retry a failed workflow")
    public ResponseEntity<Void> retryWorkflow(@PathVariable String workflowId) {
        log.info("Retrying workflow: {}", workflowId);

        RetryWorkflowCommand command = RetryWorkflowCommand.builder()
            .workflowId(workflowId)
            .build();

        orchestrationService.retryWorkflow(command);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{workflowId}/compensate")
    @Operation(summary = "Compensate (rollback) a failed workflow")
    public ResponseEntity<Void> compensateWorkflow(
            @PathVariable String workflowId,
            @RequestParam(required = false) String reason) {
        log.info("Compensating workflow: {}", workflowId);

        CompensateWorkflowCommand command = CompensateWorkflowCommand.builder()
            .workflowId(workflowId)
            .compensationReason(reason != null ? reason : "Manual compensation requested")
            .build();

        orchestrationService.compensateWorkflow(command);

        return ResponseEntity.ok().build();
    }
}