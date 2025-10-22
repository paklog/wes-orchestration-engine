package com.paklog.wes.orchestration.infrastructure.persistence.mongodb;

import com.paklog.wes.orchestration.domain.aggregate.WorkflowInstance;
import com.paklog.wes.orchestration.domain.repository.WorkflowInstanceRepository;
import com.paklog.wes.orchestration.domain.valueobject.WorkflowStatus;
import com.paklog.wes.orchestration.domain.valueobject.WorkflowType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB implementation of WorkflowInstanceRepository
 *
 * This is an infrastructure adapter that implements the domain repository port
 * following hexagonal architecture principles.
 */
@Repository
@RequiredArgsConstructor
public class MongoWorkflowInstanceRepository implements WorkflowInstanceRepository {

    private final MongoTemplate mongoTemplate;
    private final SpringDataMongoWorkflowInstanceRepository springDataRepository;

    @Override
    public WorkflowInstance save(WorkflowInstance workflowInstance) {
        return springDataRepository.save(workflowInstance);
    }

    @Override
    public Optional<WorkflowInstance> findById(String id) {
        return springDataRepository.findById(id);
    }

    @Override
    public List<WorkflowInstance> findByStatus(WorkflowStatus status) {
        Query query = new Query(Criteria.where("status").is(status));
        return mongoTemplate.find(query, WorkflowInstance.class);
    }

    @Override
    public List<WorkflowInstance> findByType(WorkflowType type) {
        Query query = new Query(Criteria.where("workflowType").is(type));
        return mongoTemplate.find(query, WorkflowInstance.class);
    }

    @Override
    public List<WorkflowInstance> findByCorrelationId(String correlationId) {
        Query query = new Query(Criteria.where("correlationId").is(correlationId));
        return mongoTemplate.find(query, WorkflowInstance.class);
    }

    @Override
    public List<WorkflowInstance> findActiveWorkflows() {
        Query query = new Query(
            Criteria.where("status").in(
                WorkflowStatus.EXECUTING,
                WorkflowStatus.PAUSED,
                WorkflowStatus.COMPENSATING
            )
        );
        return mongoTemplate.find(query, WorkflowInstance.class);
    }

    @Override
    public List<WorkflowInstance> findByCreatedAtBetween(Instant startTime, Instant endTime) {
        Query query = new Query(
            Criteria.where("createdAt")
                .gte(startTime)
                .lte(endTime)
        );
        return mongoTemplate.find(query, WorkflowInstance.class);
    }

    @Override
    public List<WorkflowInstance> findPendingWorkflows() {
        Query query = new Query(Criteria.where("status").is(WorkflowStatus.PENDING));
        query.limit(100); // Process in batches
        return mongoTemplate.find(query, WorkflowInstance.class);
    }

    @Override
    public List<WorkflowInstance> findWorkflowsForRetry() {
        Query query = new Query(
            Criteria.where("status").is(WorkflowStatus.FAILED)
                .and("retryCount").lt("maxRetries")
        );
        query.limit(50); // Process failed workflows in batches
        return mongoTemplate.find(query, WorkflowInstance.class);
    }

    @Override
    public long countByStatus(WorkflowStatus status) {
        Query query = new Query(Criteria.where("status").is(status));
        return mongoTemplate.count(query, WorkflowInstance.class);
    }

    @Override
    public void deleteById(String id) {
        springDataRepository.deleteById(id);
    }

    @Override
    public boolean existsById(String id) {
        return springDataRepository.existsById(id);
    }

    @Override
    public List<WorkflowInstance> findWorkflowsForWavelessProcessing() {
        Query query = new Query(
            Criteria.where("status").is(WorkflowStatus.EXECUTING)
                .and("workflowType").in(
                    WorkflowType.ORDER_FULFILLMENT,
                    WorkflowType.PICKING,
                    WorkflowType.PACKING
                )
                .and("priority").is("HIGH")
        );
        return mongoTemplate.find(query, WorkflowInstance.class);
    }

    @Override
    public void updateStatus(String workflowInstanceId, WorkflowStatus status) {
        Query query = new Query(Criteria.where("id").is(workflowInstanceId));
        Update update = new Update()
            .set("status", status)
            .set("updatedAt", Instant.now());

        mongoTemplate.updateFirst(query, update, WorkflowInstance.class);
    }
}