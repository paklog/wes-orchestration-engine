package com.paklog.wes.orchestration.domain.service;

import com.paklog.wes.orchestration.domain.entity.SystemLoad;
import com.paklog.wes.orchestration.domain.valueobject.LoadMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Load Balancing Domain Service
 *
 * Monitors service loads and makes rebalancing decisions
 * to optimize system performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoadBalancingService {

    private static final double TARGET_UTILIZATION = 85.0;
    private static final double CRITICAL_THRESHOLD = 95.0;
    private static final double ERROR_RATE_THRESHOLD = 0.5;

    /**
     * Monitor system load across services
     */
    public Map<String, SystemLoad> monitorSystemLoad(List<SystemLoad> servicLoads) {
        log.debug("Monitoring system load across {} services", servicLoads.size());

        return servicLoads.stream()
            .collect(Collectors.toMap(
                SystemLoad::getServiceId,
                load -> load
            ));
    }

    /**
     * Check if rebalancing is needed
     */
    public boolean needsRebalancing(Map<String, SystemLoad> serviceLoads) {
        if (serviceLoads == null || serviceLoads.isEmpty()) {
            return false;
        }

        // Check if any service is overloaded
        boolean hasOverloadedService = serviceLoads.values().stream()
            .anyMatch(SystemLoad::isOverloaded);

        if (hasOverloadedService) {
            log.warn("System has overloaded services. Rebalancing needed.");
            return true;
        }

        // Check if load distribution is uneven
        boolean unevenDistribution = hasUnevenDistribution(serviceLoads);

        if (unevenDistribution) {
            log.info("Load distribution is uneven. Rebalancing recommended.");
            return true;
        }

        return false;
    }

    /**
     * Calculate rebalancing strategy
     */
    public Map<String, Double> calculateRebalancingStrategy(Map<String, SystemLoad> serviceLoads) {
        log.info("Calculating rebalancing strategy for {} services", serviceLoads.size());

        Map<String, Double> targetLoads = new HashMap<>();

        // Calculate average load
        double averageLoad = serviceLoads.values().stream()
            .mapToDouble(SystemLoad::calculateLoad)
            .average()
            .orElse(0.0);

        log.debug("Average system load: {}%", averageLoad);

        // Set target loads (aim for target utilization)
        for (Map.Entry<String, SystemLoad> entry : serviceLoads.entrySet()) {
            String serviceId = entry.getKey();
            SystemLoad load = entry.getValue();

            double currentLoad = load.calculateLoad();
            double targetLoad;

            if (currentLoad > CRITICAL_THRESHOLD) {
                // Reduce load significantly for overloaded services
                targetLoad = TARGET_UTILIZATION * 0.8;
            } else if (currentLoad > TARGET_UTILIZATION) {
                // Gradually reduce load
                targetLoad = TARGET_UTILIZATION;
            } else if (currentLoad < TARGET_UTILIZATION * 0.5) {
                // Increase load for underutilized services
                targetLoad = TARGET_UTILIZATION * 0.7;
            } else {
                // Maintain current load
                targetLoad = currentLoad;
            }

            targetLoads.put(serviceId, targetLoad);
        }

        return targetLoads;
    }

    /**
     * Select service for new workflow
     */
    public Optional<String> selectServiceForWorkflow(Map<String, SystemLoad> serviceLoads) {
        if (serviceLoads == null || serviceLoads.isEmpty()) {
            log.warn("No services available for workflow assignment");
            return Optional.empty();
        }

        // Filter services that can accept work
        List<SystemLoad> availableServices = serviceLoads.values().stream()
            .filter(SystemLoad::canAcceptWork)
            .filter(load -> load.getErrorRate() < ERROR_RATE_THRESHOLD)
            .sorted(Comparator.comparingDouble(SystemLoad::calculateLoad))
            .toList();

        if (availableServices.isEmpty()) {
            log.warn("No services available to accept new work");
            return Optional.empty();
        }

        // Select service with lowest load
        SystemLoad selectedService = availableServices.get(0);
        log.info("Selected service {} with load {}%",
            selectedService.getServiceId(), selectedService.calculateLoad());

        return Optional.of(selectedService.getServiceId());
    }

    /**
     * Check if circuit breaker should trip
     */
    public boolean shouldTripCircuitBreaker(SystemLoad serviceLoad) {
        return serviceLoad.shouldTripCircuitBreaker(ERROR_RATE_THRESHOLD, 10);
    }

    /**
     * Update load metrics
     */
    public SystemLoad updateLoadMetrics(SystemLoad serviceLoad, LoadMetrics newMetrics) {
        serviceLoad.addMetricsSnapshot(newMetrics);

        serviceLoad.updateMetrics(
            newMetrics.getCpuUsagePercent(),
            newMetrics.getMemoryUsagePercent(),
            newMetrics.getActiveRequests(),
            newMetrics.getQueueDepth(),
            newMetrics.getAverageResponseTimeMs(),
            newMetrics.getErrorRate()
        );

        return serviceLoad;
    }

    /**
     * Get health status of service
     */
    public String getServiceHealthStatus(SystemLoad serviceLoad) {
        double load = serviceLoad.calculateLoad();

        if (load >= CRITICAL_THRESHOLD) {
            return "CRITICAL";
        } else if (load >= TARGET_UTILIZATION) {
            return "WARNING";
        } else if (serviceLoad.getErrorRate() > ERROR_RATE_THRESHOLD) {
            return "DEGRADED";
        } else {
            return "HEALTHY";
        }
    }

    /**
     * Calculate available capacity
     */
    public double calculateAvailableCapacity(Map<String, SystemLoad> serviceLoads) {
        if (serviceLoads == null || serviceLoads.isEmpty()) {
            return 0.0;
        }

        return serviceLoads.values().stream()
            .mapToDouble(SystemLoad::getAvailableCapacity)
            .average()
            .orElse(0.0);
    }

    /**
     * Get service load trend
     */
    public String getLoadTrend(SystemLoad serviceLoad) {
        if (serviceLoad.isLoadIncreasing()) {
            return "INCREASING";
        } else {
            double currentLoad = serviceLoad.calculateLoad();
            double averageLoad = serviceLoad.calculateAverageLoad(5); // 5 minute window

            if (currentLoad < averageLoad * 0.9) {
                return "DECREASING";
            } else {
                return "STABLE";
            }
        }
    }

    // Private helper methods

    private boolean hasUnevenDistribution(Map<String, SystemLoad> serviceLoads) {
        if (serviceLoads.size() < 2) {
            return false;
        }

        double[] loads = serviceLoads.values().stream()
            .mapToDouble(SystemLoad::calculateLoad)
            .toArray();

        double min = Arrays.stream(loads).min().orElse(0.0);
        double max = Arrays.stream(loads).max().orElse(0.0);

        // Consider distribution uneven if difference exceeds 30%
        return (max - min) > 30.0;
    }
}
