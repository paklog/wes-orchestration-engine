package com.paklog.wes.orchestration.domain.valueobject;

/**
 * Workflow step execution status enumeration
 */
public enum StepStatus {
    PENDING("Step is pending execution"),
    EXECUTING("Step is currently executing"),
    COMPLETED("Step completed successfully"),
    FAILED("Step execution failed"),
    COMPENSATING("Step is being compensated (rolled back)"),
    COMPENSATED("Step compensation completed"),
    SKIPPED("Step was skipped");

    private final String description;

    StepStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == COMPENSATED || this == SKIPPED;
    }

    public boolean isActive() {
        return this == EXECUTING || this == COMPENSATING;
    }

    public boolean canRetry() {
        return this == FAILED;
    }

    public boolean canCompensate() {
        return this == COMPLETED;
    }
}
