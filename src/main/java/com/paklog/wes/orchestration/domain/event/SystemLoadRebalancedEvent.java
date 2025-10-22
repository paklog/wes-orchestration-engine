package com.paklog.wes.orchestration.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;

/**
 * Event emitted when system load is rebalanced
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SystemLoadRebalancedEvent extends DomainEvent {

    private String serviceId;
    private Double previousLoad;
    private Double currentLoad;
    private Map<String, Double> serviceLoads;
    private Instant rebalancedAt;
    private String reason;

    public SystemLoadRebalancedEvent(String serviceId, Double previousLoad, Double currentLoad) {
        super("SystemLoadRebalanced", serviceId);
        this.serviceId = serviceId;
        this.previousLoad = previousLoad;
        this.currentLoad = currentLoad;
        this.rebalancedAt = Instant.now();
    }
}
