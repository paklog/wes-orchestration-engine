package com.paklog.wes.orchestration.infrastructure.persistence.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * MongoDB document for WorkflowDefinition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workflow_definitions")
public class WorkflowDefinitionDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String workflowName;

    private String workflowType;
    private String description;
    private String version;
    private List<StepDefinitionDocument> steps;
    private Map<String, Object> defaultParameters;
    private Long defaultTimeoutMs;
    private Integer maxRetries;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepDefinitionDocument {
        private String stepId;
        private String stepName;
        private String stepType;
        private String serviceName;
        private String operation;
        private Integer order;
        private List<String> dependencies;
        private Long timeoutMs;
        private Map<String, Object> configuration;
        private Boolean compensable;
        private String compensationOperation;
    }
}
