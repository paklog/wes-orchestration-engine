package com.paklog.wes.orchestration.infrastructure.web.controller;

import com.paklog.wes.orchestration.application.command.*;
import com.paklog.wes.orchestration.application.port.in.OrchestrationUseCase;
import com.paklog.wes.orchestration.domain.aggregate.WorkflowInstance;
import com.paklog.wes.orchestration.infrastructure.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for workflow orchestration operations
 */
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Workflow Orchestration", description = "WES orchestration engine API")
public class WorkflowController {

    private final OrchestrationUseCase orchestrationUseCase;
    private final WorkflowMapper workflowMapper;

    @PostMapping
    @Operation(summary = "Start a new workflow instance")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Workflow started successfully",
            content = @Content(schema = @Schema(implementation = WorkflowInstanceDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<WorkflowInstanceDto> startWorkflow(
            @Valid @RequestBody StartWorkflowRequest request) {
        log.info("Starting workflow: {}", request.getWorkflowName());

        StartWorkflowCommand command = workflowMapper.toCommand(request);
        WorkflowInstance instance = orchestrationUseCase.startWorkflow(command);
        WorkflowInstanceDto response = workflowMapper.toDto(instance);

        log.info("Workflow started successfully with ID: {}", instance.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{workflowId}/steps/execute")
    @Operation(summary = "Execute a step in a workflow")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Step executed successfully"),
        @ApiResponse(responseCode = "404", description = "Workflow not found"),
        @ApiResponse(responseCode = "400", description = "Invalid step execution request")
    })
    public ResponseEntity<Void> executeStep(
            @PathVariable String workflowId,
            @Valid @RequestBody ExecuteStepRequest request) {
        log.info("Executing step {} in workflow {}", request.getStepId(), workflowId);

        ExecuteStepCommand command = ExecuteStepCommand.builder()
            .workflowInstanceId(workflowId)
            .stepId(request.getStepId())
            .result(request.getResult())
            .build();

        orchestrationUseCase.executeStep(command);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{workflowId}/pause")
    @Operation(summary = "Pause a running workflow")
    public ResponseEntity<Void> pauseWorkflow(@PathVariable String workflowId) {
        log.info("Pausing workflow: {}", workflowId);

        PauseWorkflowCommand command = PauseWorkflowCommand.builder()
            .workflowInstanceId(workflowId)
            .build();

        orchestrationUseCase.pauseWorkflow(command);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{workflowId}/resume")
    @Operation(summary = "Resume a paused workflow")
    public ResponseEntity<Void> resumeWorkflow(@PathVariable String workflowId) {
        log.info("Resuming workflow: {}", workflowId);

        ResumeWorkflowCommand command = ResumeWorkflowCommand.builder()
            .workflowInstanceId(workflowId)
            .build();

        orchestrationUseCase.resumeWorkflow(command);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{workflowId}/cancel")
    @Operation(summary = "Cancel a workflow")
    public ResponseEntity<Void> cancelWorkflow(
            @PathVariable String workflowId,
            @RequestParam(required = false) String reason) {
        log.info("Cancelling workflow: {} with reason: {}", workflowId, reason);

        CancelWorkflowCommand command = CancelWorkflowCommand.builder()
            .workflowInstanceId(workflowId)
            .reason(reason != null ? reason : "User requested cancellation")
            .build();

        orchestrationUseCase.cancelWorkflow(command);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{workflowId}/retry")
    @Operation(summary = "Retry a failed workflow")
    public ResponseEntity<Void> retryWorkflow(@PathVariable String workflowId) {
        log.info("Retrying workflow: {}", workflowId);

        RetryWorkflowCommand command = RetryWorkflowCommand.builder()
            .workflowInstanceId(workflowId)
            .build();

        orchestrationUseCase.retryWorkflow(command);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{workflowId}/complete")
    @Operation(summary = "Complete a workflow")
    public ResponseEntity<Void> completeWorkflow(
            @PathVariable String workflowId,
            @RequestBody(required = false) CompleteWorkflowRequest request) {
        log.info("Completing workflow: {}", workflowId);

        CompleteWorkflowCommand command = CompleteWorkflowCommand.builder()
            .workflowInstanceId(workflowId)
            .outputParameters(request != null ? request.getOutputParameters() : null)
            .build();

        orchestrationUseCase.completeWorkflow(command);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{workflowId}/compensate")
    @Operation(summary = "Compensate (rollback) a failed workflow")
    public ResponseEntity<Void> compensateWorkflow(@PathVariable String workflowId) {
        log.info("Compensating workflow: {}", workflowId);

        CompensateWorkflowCommand command = CompensateWorkflowCommand.builder()
            .workflowInstanceId(workflowId)
            .build();

        orchestrationUseCase.compensateWorkflow(command);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{workflowId}/waveless")
    @Operation(summary = "Transition workflow to waveless processing")
    public ResponseEntity<Void> transitionToWaveless(@PathVariable String workflowId) {
        log.info("Transitioning workflow {} to waveless processing", workflowId);

        TransitionToWavelessCommand command = TransitionToWavelessCommand.builder()
            .workflowInstanceId(workflowId)
            .build();

        orchestrationUseCase.transitionToWaveless(command);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/rebalance")
    @Operation(summary = "Rebalance system load across workflows")
    public ResponseEntity<Void> rebalanceSystemLoad() {
        log.info("Rebalancing system load");

        RebalanceLoadCommand command = RebalanceLoadCommand.builder()
            .targetUtilization(0.85) // 85% target utilization
            .build();

        orchestrationUseCase.rebalanceSystemLoad(command);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    @Operation(summary = "Check orchestration engine health")
    public ResponseEntity<OrchestrationHealthDto> getHealth() {
        // This would typically check various health metrics
        OrchestrationHealthDto health = OrchestrationHealthDto.builder()
            .status("UP")
            .activeWorkflows(42) // These would be real metrics
            .queueDepth(15)
            .averageExecutionTime(1250L)
            .build();

        return ResponseEntity.ok(health);
    }
}