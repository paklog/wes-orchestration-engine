package com.paklog.wes.orchestration.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Compensation action definition for saga pattern rollback
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationAction implements Serializable {

    private String actionId;
    private String stepId;
    private String serviceName;
    private String operation;
    private Map<String, Object> parameters;
    private CompensationStrategy strategy;
    private Boolean idempotent;
    private Integer maxRetries;
    private Long timeoutMs;

    /**
     * Compensation strategy enumeration
     */
    public enum CompensationStrategy {
        REVERSE_OPERATION("Reverse the original operation"),
        DELETE_CREATED("Delete created resources"),
        RESTORE_STATE("Restore previous state"),
        CUSTOM("Custom compensation logic");

        private final String description;

        CompensationStrategy(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Check if action is ready for execution
     */
    public boolean isValid() {
        return actionId != null && !actionId.isBlank()
            && stepId != null && !stepId.isBlank()
            && serviceName != null && !serviceName.isBlank()
            && operation != null && !operation.isBlank()
            && strategy != null;
    }

    /**
     * Create reverse operation compensation
     */
    public static CompensationAction reverseOperation(String stepId, String serviceName,
                                                      String operation, Map<String, Object> parameters) {
        return CompensationAction.builder()
            .actionId("comp-" + stepId)
            .stepId(stepId)
            .serviceName(serviceName)
            .operation(operation)
            .parameters(parameters)
            .strategy(CompensationStrategy.REVERSE_OPERATION)
            .idempotent(true)
            .maxRetries(3)
            .timeoutMs(5000L)
            .build();
    }

    /**
     * Create delete resources compensation
     */
    public static CompensationAction deleteCreated(String stepId, String serviceName,
                                                   String resourceId) {
        return CompensationAction.builder()
            .actionId("comp-" + stepId)
            .stepId(stepId)
            .serviceName(serviceName)
            .operation("delete")
            .parameters(Map.of("resourceId", resourceId))
            .strategy(CompensationStrategy.DELETE_CREATED)
            .idempotent(true)
            .maxRetries(3)
            .timeoutMs(5000L)
            .build();
    }

    /**
     * Create state restoration compensation
     */
    public static CompensationAction restoreState(String stepId, String serviceName,
                                                  Map<String, Object> previousState) {
        return CompensationAction.builder()
            .actionId("comp-" + stepId)
            .stepId(stepId)
            .serviceName(serviceName)
            .operation("restore")
            .parameters(previousState)
            .strategy(CompensationStrategy.RESTORE_STATE)
            .idempotent(true)
            .maxRetries(3)
            .timeoutMs(5000L)
            .build();
    }
}
