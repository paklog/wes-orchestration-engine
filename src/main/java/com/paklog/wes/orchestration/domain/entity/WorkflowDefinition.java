package com.paklog.wes.orchestration.domain.entity;

import com.paklog.wes.orchestration.domain.valueobject.CompensationAction;
import com.paklog.wes.orchestration.domain.valueobject.RetryPolicy;
import com.paklog.wes.orchestration.domain.valueobject.WorkflowType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Workflow Definition Entity
 *
 * Defines the template for workflow execution including steps,
 * compensation logic, and execution rules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinition {

    private String definitionId;
    private String name;
    private String description;
    private WorkflowType workflowType;
    private String version;

    @Builder.Default
    private List<StepDefinition> steps = new ArrayList<>();

    @Builder.Default
    private Map<String, CompensationAction> compensationSteps = Map.of();

    private Long timeoutMs;
    private Integer maxRetries;
    private RetryPolicy defaultRetryPolicy;

    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;

    /**
     * Step definition within workflow
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepDefinition {
        private String stepId;
        private String stepName;
        private String stepType;
        private String serviceName;
        private String operation;
        private Integer executionOrder;
        private Map<String, Object> defaultInputs;
        private Long timeoutMs;
        private RetryPolicy retryPolicy;
        private CompensationAction compensationAction;
        private List<String> dependencies;
        private Map<String, String> conditions;
        private Boolean optional;
    }

    /**
     * Validate workflow definition
     */
    public boolean validate() {
        if (definitionId == null || definitionId.isBlank()) {
            return false;
        }

        if (name == null || name.isBlank()) {
            return false;
        }

        if (workflowType == null) {
            return false;
        }

        if (steps == null || steps.isEmpty()) {
            return false;
        }

        // Validate step execution order
        List<Integer> orders = steps.stream()
            .map(StepDefinition::getExecutionOrder)
            .sorted()
            .collect(Collectors.toList());

        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i) != i + 1) {
                return false; // Execution order must be sequential starting from 1
            }
        }

        // Validate dependencies
        List<String> stepIds = steps.stream()
            .map(StepDefinition::getStepId)
            .collect(Collectors.toList());

        for (StepDefinition step : steps) {
            if (step.getDependencies() != null) {
                for (String dependency : step.getDependencies()) {
                    if (!stepIds.contains(dependency)) {
                        return false; // Dependency step not found
                    }
                }
            }
        }

        return true;
    }

    /**
     * Get next step based on current step
     */
    public Optional<StepDefinition> getNextStep(String currentStepId) {
        if (currentStepId == null) {
            // Return first step
            return steps.stream()
                .min((s1, s2) -> s1.getExecutionOrder().compareTo(s2.getExecutionOrder()));
        }

        Optional<StepDefinition> currentStep = steps.stream()
            .filter(s -> s.getStepId().equals(currentStepId))
            .findFirst();

        if (currentStep.isEmpty()) {
            return Optional.empty();
        }

        int currentOrder = currentStep.get().getExecutionOrder();
        return steps.stream()
            .filter(s -> s.getExecutionOrder() == currentOrder + 1)
            .findFirst();
    }

    /**
     * Get compensation step for given step
     */
    public Optional<CompensationAction> getCompensationStep(String stepId) {
        if (compensationSteps == null || compensationSteps.isEmpty()) {
            // Try to get from step definition
            return steps.stream()
                .filter(s -> s.getStepId().equals(stepId))
                .map(StepDefinition::getCompensationAction)
                .findFirst();
        }

        return Optional.ofNullable(compensationSteps.get(stepId));
    }

    /**
     * Get steps in execution order
     */
    public List<StepDefinition> getStepsInOrder() {
        return steps.stream()
            .sorted((s1, s2) -> s1.getExecutionOrder().compareTo(s2.getExecutionOrder()))
            .collect(Collectors.toList());
    }

    /**
     * Get steps in reverse order (for compensation)
     */
    public List<StepDefinition> getStepsInReverseOrder() {
        return steps.stream()
            .sorted((s1, s2) -> s2.getExecutionOrder().compareTo(s1.getExecutionOrder()))
            .collect(Collectors.toList());
    }

    /**
     * Check if step has dependencies
     */
    public boolean hasDependencies(String stepId) {
        return steps.stream()
            .filter(s -> s.getStepId().equals(stepId))
            .findFirst()
            .map(s -> s.getDependencies() != null && !s.getDependencies().isEmpty())
            .orElse(false);
    }

    /**
     * Get dependencies for step
     */
    public List<String> getDependencies(String stepId) {
        return steps.stream()
            .filter(s -> s.getStepId().equals(stepId))
            .findFirst()
            .map(StepDefinition::getDependencies)
            .orElse(List.of());
    }

    /**
     * Check if all dependencies are satisfied
     */
    public boolean areDependenciesSatisfied(String stepId, List<String> completedSteps) {
        List<String> dependencies = getDependencies(stepId);
        return completedSteps.containsAll(dependencies);
    }

    /**
     * Get step by ID
     */
    public Optional<StepDefinition> getStep(String stepId) {
        return steps.stream()
            .filter(s -> s.getStepId().equals(stepId))
            .findFirst();
    }

    /**
     * Get first step
     */
    public Optional<StepDefinition> getFirstStep() {
        return getNextStep(null);
    }

    /**
     * Check if step is last
     */
    public boolean isLastStep(String stepId) {
        Optional<StepDefinition> step = getStep(stepId);
        if (step.isEmpty()) {
            return false;
        }

        int maxOrder = steps.stream()
            .mapToInt(StepDefinition::getExecutionOrder)
            .max()
            .orElse(0);

        return step.get().getExecutionOrder() == maxOrder;
    }

    /**
     * Get total step count
     */
    public int getTotalSteps() {
        return steps != null ? steps.size() : 0;
    }

    /**
     * Check if definition is active
     */
    public boolean isActive() {
        return active != null && active;
    }
}
