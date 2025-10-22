package com.paklog.wes.orchestration.infrastructure.persistence.mongodb;

import com.paklog.wes.orchestration.domain.aggregate.WorkflowInstance;
import com.paklog.wes.orchestration.domain.repository.WorkflowInstanceRepository;
import com.paklog.wes.orchestration.domain.valueobject.WorkflowStatus;
import com.paklog.wes.orchestration.domain.valueobject.WorkflowType;
import com.paklog.wes.orchestration.infrastructure.persistence.mongodb.document.WorkflowInstanceDocument;
import com.paklog.wes.orchestration.infrastructure.persistence.mongodb.mapper.WorkflowMapper;
import com.paklog.wes.orchestration.infrastructure.persistence.mongodb.repository.SpringDataMongoWorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of WorkflowInstanceRepository
 *
 * This is an infrastructure adapter that implements the domain repository port
 * following hexagonal architecture principles.
 */
@Repository
@RequiredArgsConstructor
public class MongoWorkflowInstanceRepository implements WorkflowInstanceRepository {

    private final SpringDataMongoWorkflowRepository springDataRepository;
    private final WorkflowMapper workflowMapper;

    @Override
    public WorkflowInstance save(WorkflowInstance workflowInstance) {
        WorkflowInstanceDocument document = workflowMapper.toDocument(workflowInstance);
        WorkflowInstanceDocument savedDocument = springDataRepository.save(document);
        return workflowMapper.toDomain(savedDocument);
    }

    @Override
    public Optional<WorkflowInstance> findById(String id) {
        return springDataRepository.findById(id)
            .map(workflowMapper::toDomain);
    }

    @Override
    public List<WorkflowInstance> findByStatus(WorkflowStatus status) {
        return springDataRepository.findByStatus(status.name()).stream()
            .map(workflowMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<WorkflowInstance> findByType(WorkflowType type) {
        return springDataRepository.findByWorkflowType(type.name()).stream()
            .map(workflowMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<WorkflowInstance> findByCorrelationId(String correlationId) {
        return springDataRepository.findByCorrelationId(correlationId).stream()
            .map(workflowMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<WorkflowInstance> findActiveWorkflows() {
        return springDataRepository.findActiveWorkflows().stream()
            .map(workflowMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<WorkflowInstance> findByCreatedAtBetween(Instant startTime, Instant endTime) {
        return springDataRepository.findByCreatedAtBetween(startTime, endTime).stream()
            .map(workflowMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<WorkflowInstance> findPendingWorkflows() {
        return springDataRepository.findPendingWorkflows().stream()
            .map(workflowMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<WorkflowInstance> findWorkflowsForRetry() {
        return springDataRepository.findWorkflowsForRetry().stream()
            .map(workflowMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public long countByStatus(WorkflowStatus status) {
        return springDataRepository.countByStatus(status.name());
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
        return springDataRepository.findWorkflowsForWavelessProcessing().stream()
            .map(workflowMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void updateStatus(String workflowInstanceId, WorkflowStatus status) {
        Optional<WorkflowInstanceDocument> documentOpt = springDataRepository.findById(workflowInstanceId);
        if (documentOpt.isPresent()) {
            WorkflowInstanceDocument document = documentOpt.get();
            document.setStatus(status.name());
            document.setUpdatedAt(Instant.now());
            springDataRepository.save(document);
        }
    }
}