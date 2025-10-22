package com.paklog.wes.orchestration.domain.valueobject;

/**
 * Workflow priority enumeration for waveless processing
 */
public enum WorkflowPriority {
    HIGH(1, "High priority - process immediately"),
    NORMAL(2, "Normal priority"),
    LOW(3, "Low priority");

    private final int level;
    private final String description;

    WorkflowPriority(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHigherThan(WorkflowPriority other) {
        return this.level < other.level;
    }

    public boolean isLowerThan(WorkflowPriority other) {
        return this.level > other.level;
    }
}
