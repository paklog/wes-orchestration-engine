package com.paklog.wes.orchestration.infrastructure.web.controller;

import com.paklog.wes.orchestration.application.command.RebalanceSystemLoadCommand;
import com.paklog.wes.orchestration.application.service.WavelessOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Waveless Processing", description = "Waveless processing operations")
public class WavelessController {

    private final WavelessOrchestrationService wavelessService;

    @PutMapping("/{id}/waveless")
    @Operation(summary = "Enable waveless processing for workflow")
    public ResponseEntity<Void> enableWaveless(@PathVariable String id) {
        log.info("Enabling waveless processing for workflow: {}", id);

        wavelessService.enableWavelessProcessing(id);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/rebalance")
    @Operation(summary = "Rebalance system load")
    public ResponseEntity<Void> rebalanceLoad(
            @RequestParam(defaultValue = "0.85") Double targetUtilization) {
        log.info("Rebalancing system load with target: {}", targetUtilization);

        RebalanceSystemLoadCommand command = RebalanceSystemLoadCommand.builder()
            .targetUtilization(targetUtilization)
            .build();

        wavelessService.rebalanceSystemLoad(command);

        return ResponseEntity.ok().build();
    }
}
