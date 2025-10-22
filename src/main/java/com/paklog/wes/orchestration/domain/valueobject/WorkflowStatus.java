package com.paklog.wes.orchestration.domain.valueobject;

/**
 * Workflow execution status enumeration
 */
public enum WorkflowStatus {
    PENDING("Workflow is pending execution"),
    EXECUTING("Workflow is currently executing"),
    PAUSED("Workflow execution is paused"),
    COMPLETED("Workflow completed successfully"),
    FAILED("Workflow execution failed"),
    COMPENSATING("Workflow is being compensated (rolled back)"),
    COMPENSATED("Workflow compensation completed"),
    CANCELLED("Workflow was cancelled");

    private final String description;

    WorkflowStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == COMPENSATED || this == CANCELLED;
    }

    public boolean isActive() {
        return this == EXECUTING || this == PAUSED || this == COMPENSATING;
    }
}