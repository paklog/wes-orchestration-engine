package com.paklog.wes.orchestration.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Event emitted when waveless processing is enabled for a workflow
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WavelessProcessingEnabledEvent extends DomainEvent {

    private String workflowInstanceId;
    private Integer batchSize;
    private Long processingIntervalMs;
    private Instant enabledAt;

    public WavelessProcessingEnabledEvent(String workflowInstanceId, Integer batchSize,
                                          Long processingIntervalMs) {
        super("WavelessProcessingEnabled", workflowInstanceId);
        this.workflowInstanceId = workflowInstanceId;
        this.batchSize = batchSize;
        this.processingIntervalMs = processingIntervalMs;
        this.enabledAt = Instant.now();
    }
}
