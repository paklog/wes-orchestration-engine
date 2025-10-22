package com.paklog.wes.orchestration.infrastructure.persistence.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document for SystemLoad tracking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "system_loads")
public class SystemLoadDocument {

    @Id
    private String id;

    @Indexed
    private String workflowInstanceId;

    private Integer activeSteps;
    private Integer totalSteps;
    private Double utilizationPercentage;
    private Integer activeWorkflows;
    private Integer totalWorkflows;
    private Double systemUtilization;

    @Indexed
    private Instant timestamp;

    private String nodeId;
    private Map<String, Object> additionalMetrics;

    private static class Map<K, V> extends java.util.HashMap<K, V> {
    }
}
