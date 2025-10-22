package com.paklog.wes.orchestration.domain.repository;

import com.paklog.wes.orchestration.domain.aggregate.WorkflowInstance;
import com.paklog.wes.orchestration.domain.valueobject.WorkflowStatus;
import com.paklog.wes.orchestration.domain.valueobject.WorkflowType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for WorkflowInstance aggregate
 *
 * This is a domain repository interface (port) that will be implemented
 * by the infrastructure layer following hexagonal architecture.
 */
public interface WorkflowInstanceRepository {

    /**
     * Save a workflow instance
     */
    WorkflowInstance save(WorkflowInstance workflowInstance);

    /**
     * Find workflow instance by ID
     */
    Optional<WorkflowInstance> findById(String id);

    /**
     * Find workflow instances by status
     */
    List<WorkflowInstance> findByStatus(WorkflowStatus status);

    /**
     * Find workflow instances by type
     */
    List<WorkflowInstance> findByType(WorkflowType type);

    /**
     * Find workflow instances by correlation ID
     */
    List<WorkflowInstance> findByCorrelationId(String correlationId);

    /**
     * Find active workflow instances
     */
    List<WorkflowInstance> findActiveWorkflows();

    /**
     * Find workflow instances created within a time range
     */
    List<WorkflowInstance> findByCreatedAtBetween(Instant startTime, Instant endTime);

    /**
     * Find workflow instances ready for execution
     */
    List<WorkflowInstance> findPendingWorkflows();

    /**
     * Find workflow instances that need retry
     */
    List<WorkflowInstance> findWorkflowsForRetry();

    /**
     * Count workflow instances by status
     */
    long countByStatus(WorkflowStatus status);

    /**
     * Delete a workflow instance
     */
    void deleteById(String id);

    /**
     * Check if workflow instance exists
     */
    boolean existsById(String id);

    /**
     * Find workflow instances for waveless processing
     */
    List<WorkflowInstance> findWorkflowsForWavelessProcessing();

    /**
     * Update workflow status
     */
    void updateStatus(String workflowInstanceId, WorkflowStatus status);
}