package com.paklog.wes.orchestration.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Workflow execution error information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowError implements Serializable {

    private String errorId;
    private String stepId;
    private String errorCode;
    private String errorMessage;
    private String errorType;
    private String serviceName;
    private Instant occurredAt;
    private Boolean recoverable;
    private String stackTrace;

    /**
     * Error types
     */
    public enum ErrorType {
        VALIDATION_ERROR,
        SERVICE_UNAVAILABLE,
        TIMEOUT,
        BUSINESS_RULE_VIOLATION,
        DATA_INTEGRITY_ERROR,
        NETWORK_ERROR,
        PERMISSION_DENIED,
        RESOURCE_NOT_FOUND,
        INTERNAL_ERROR,
        COMPENSATION_FAILED
    }

    /**
     * Create error from exception
     */
    public static WorkflowError fromException(String stepId, String serviceName, Exception exception) {
        return WorkflowError.builder()
            .errorId(generateErrorId())
            .stepId(stepId)
            .errorCode(exception.getClass().getSimpleName())
            .errorMessage(exception.getMessage())
            .errorType(determineErrorType(exception))
            .serviceName(serviceName)
            .occurredAt(Instant.now())
            .recoverable(isRecoverable(exception))
            .stackTrace(getStackTraceAsString(exception))
            .build();
    }

    /**
     * Create error with details
     */
    public static WorkflowError create(String stepId, String serviceName, String errorCode,
                                       String errorMessage, String errorType, boolean recoverable) {
        return WorkflowError.builder()
            .errorId(generateErrorId())
            .stepId(stepId)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .errorType(errorType)
            .serviceName(serviceName)
            .occurredAt(Instant.now())
            .recoverable(recoverable)
            .build();
    }

    /**
     * Check if error is recoverable
     */
    public boolean isRecoverable() {
        return recoverable != null && recoverable;
    }

    /**
     * Check if error requires compensation
     */
    public boolean requiresCompensation() {
        return !isRecoverable() && !errorType.equals(ErrorType.VALIDATION_ERROR.name());
    }

    // Private helper methods

    private static String generateErrorId() {
        return "err-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 10000);
    }

    private static String determineErrorType(Exception exception) {
        String className = exception.getClass().getSimpleName().toUpperCase();

        if (className.contains("TIMEOUT")) {
            return ErrorType.TIMEOUT.name();
        } else if (className.contains("VALIDATION")) {
            return ErrorType.VALIDATION_ERROR.name();
        } else if (className.contains("NOTFOUND") || className.contains("NOT_FOUND")) {
            return ErrorType.RESOURCE_NOT_FOUND.name();
        } else if (className.contains("PERMISSION") || className.contains("FORBIDDEN")) {
            return ErrorType.PERMISSION_DENIED.name();
        } else if (className.contains("NETWORK") || className.contains("CONNECTION")) {
            return ErrorType.NETWORK_ERROR.name();
        } else {
            return ErrorType.INTERNAL_ERROR.name();
        }
    }

    private static boolean isRecoverable(Exception exception) {
        String errorType = determineErrorType(exception);
        return errorType.equals(ErrorType.TIMEOUT.name())
            || errorType.equals(ErrorType.SERVICE_UNAVAILABLE.name())
            || errorType.equals(ErrorType.NETWORK_ERROR.name());
    }

    private static String getStackTraceAsString(Exception exception) {
        StringBuilder sb = new StringBuilder();
        sb.append(exception.toString()).append("\n");
        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
