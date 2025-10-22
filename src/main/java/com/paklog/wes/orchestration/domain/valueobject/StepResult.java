package com.paklog.wes.orchestration.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Result of a workflow step execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepResult implements Serializable {

    private String stepId;
    private Boolean success;
    private Map<String, Object> outputData;
    private String errorMessage;
    private String errorCode;
    private Long executionTimeMs;
    private Instant completedAt;
    private Map<String, String> metadata;

    /**
     * Create successful result
     */
    public static StepResult success(String stepId, Map<String, Object> outputData, long executionTimeMs) {
        return StepResult.builder()
            .stepId(stepId)
            .success(true)
            .outputData(outputData)
            .executionTimeMs(executionTimeMs)
            .completedAt(Instant.now())
            .build();
    }

    /**
     * Create failed result
     */
    public static StepResult failure(String stepId, String errorMessage, String errorCode) {
        return StepResult.builder()
            .stepId(stepId)
            .success(false)
            .errorMessage(errorMessage)
            .errorCode(errorCode)
            .completedAt(Instant.now())
            .build();
    }

    /**
     * Create failed result with execution time
     */
    public static StepResult failure(String stepId, String errorMessage, String errorCode, long executionTimeMs) {
        return StepResult.builder()
            .stepId(stepId)
            .success(false)
            .errorMessage(errorMessage)
            .errorCode(errorCode)
            .executionTimeMs(executionTimeMs)
            .completedAt(Instant.now())
            .build();
    }

    /**
     * Check if result indicates success
     */
    public boolean isSuccess() {
        return success != null && success;
    }

    /**
     * Check if result indicates failure
     */
    public boolean isFailure() {
        return success == null || !success;
    }

    /**
     * Get output value by key
     */
    public Object getOutput(String key) {
        return outputData != null ? outputData.get(key) : null;
    }

    /**
     * Check if execution time exceeded threshold
     */
    public boolean exceededTimeout(long timeoutMs) {
        return executionTimeMs != null && executionTimeMs > timeoutMs;
    }
}
