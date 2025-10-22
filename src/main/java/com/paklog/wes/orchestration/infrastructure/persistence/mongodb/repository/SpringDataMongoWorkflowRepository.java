package com.paklog.wes.orchestration.infrastructure.persistence.mongodb.repository;

import com.paklog.wes.orchestration.infrastructure.persistence.mongodb.document.WorkflowInstanceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data MongoDB repository for WorkflowInstanceDocument
 */
@Repository
public interface SpringDataMongoWorkflowRepository extends MongoRepository<WorkflowInstanceDocument, String> {

    /**
     * Find workflows by status
     */
    List<WorkflowInstanceDocument> findByStatus(String status);

    /**
     * Find workflows by type
     */
    List<WorkflowInstanceDocument> findByWorkflowType(String workflowType);

    /**
     * Find workflows by correlation ID
     */
    List<WorkflowInstanceDocument> findByCorrelationId(String correlationId);

    /**
     * Find active workflows (EXECUTING, PAUSED, or COMPENSATING status)
     */
    @Query("{ 'status': { $in: ['EXECUTING', 'PAUSED', 'COMPENSATING'] } }")
    List<WorkflowInstanceDocument> findActiveWorkflows();

    /**
     * Find workflows created within a time range
     */
    List<WorkflowInstanceDocument> findByCreatedAtBetween(Instant startTime, Instant endTime);

    /**
     * Find pending workflows
     */
    @Query("{ 'status': 'PENDING' }")
    List<WorkflowInstanceDocument> findPendingWorkflows();

    /**
     * Find workflows that need retry (FAILED status with retryCount < maxRetries)
     */
    @Query("{ 'status': 'FAILED', $expr: { $lt: ['$retryCount', '$maxRetries'] } }")
    List<WorkflowInstanceDocument> findWorkflowsForRetry();

    /**
     * Count workflows by status
     */
    long countByStatus(String status);

    /**
     * Find workflows for waveless processing (HIGH priority, PENDING or EXECUTING status)
     */
    @Query("{ $and: [ { 'priority': 'HIGH' }, { 'status': { $in: ['PENDING', 'EXECUTING'] } } ] }")
    List<WorkflowInstanceDocument> findWorkflowsForWavelessProcessing();

    /**
     * Find workflows by priority
     */
    List<WorkflowInstanceDocument> findByPriority(String priority);

    /**
     * Find workflows that have been executing for longer than timeout
     */
    @Query("{ 'status': 'EXECUTING', 'startedAt': { $lt: ?0 } }")
    List<WorkflowInstanceDocument> findTimedOutWorkflows(Instant timeoutThreshold);
}
