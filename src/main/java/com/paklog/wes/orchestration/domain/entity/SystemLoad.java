package com.paklog.wes.orchestration.domain.entity;

import com.paklog.wes.orchestration.domain.valueobject.LoadMetrics;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * System Load Entity
 *
 * Tracks system load across services for load balancing decisions.
 * Maintains historical metrics for trend analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLoad {

    private String id;
    private String serviceId;
    private String serviceName;
    private String workflowInstanceId;

    private Double cpuUsage;
    private Double memoryUsage;
    private Integer activeRequests;
    private Integer queueDepth;
    private Long responseTime;
    private Double errorRate;

    private Integer activeSteps;
    private Integer totalSteps;
    private Double utilizationPercentage;

    private Instant timestamp;

    @Builder.Default
    private List<LoadMetrics> historicalMetrics = new ArrayList<>();

    @Builder.Default
    private Double targetUtilization = 85.0;

    @Builder.Default
    private Double criticalThreshold = 95.0;

    /**
     * Calculate overall load score
     */
    public double calculateLoad() {
        double cpuScore = cpuUsage != null ? cpuUsage : 0.0;
        double memoryScore = memoryUsage != null ? memoryUsage : 0.0;
        double utilizationScore = utilizationPercentage != null ? utilizationPercentage : 0.0;
        double errorScore = (errorRate != null ? errorRate : 0.0) * 100;

        // Weighted average
        return (cpuScore * 0.3) + (memoryScore * 0.25) + (utilizationScore * 0.25) + (errorScore * 0.2);
    }

    /**
     * Check if service is overloaded
     */
    public boolean isOverloaded() {
        return calculateLoad() >= criticalThreshold;
    }

    /**
     * Check if service needs rebalancing
     */
    public boolean needsRebalancing() {
        double currentLoad = calculateLoad();
        return currentLoad >= targetUtilization || errorRate > 0.5;
    }

    /**
     * Check if service can accept more work
     */
    public boolean canAcceptWork() {
        return calculateLoad() < targetUtilization && errorRate < 0.3;
    }

    /**
     * Add metrics snapshot to history
     */
    public void addMetricsSnapshot(LoadMetrics metrics) {
        if (historicalMetrics == null) {
            historicalMetrics = new ArrayList<>();
        }

        historicalMetrics.add(metrics);

        // Keep only last 100 snapshots
        if (historicalMetrics.size() > 100) {
            historicalMetrics.remove(0);
        }
    }

    /**
     * Calculate average load over time window
     */
    public double calculateAverageLoad(long timeWindowMinutes) {
        if (historicalMetrics == null || historicalMetrics.isEmpty()) {
            return calculateLoad();
        }

        Instant cutoff = Instant.now().minusSeconds(timeWindowMinutes * 60);

        List<LoadMetrics> recentMetrics = historicalMetrics.stream()
            .filter(m -> m.getTimestamp().isAfter(cutoff))
            .toList();

        if (recentMetrics.isEmpty()) {
            return calculateLoad();
        }

        return recentMetrics.stream()
            .mapToDouble(LoadMetrics::calculateLoadScore)
            .average()
            .orElse(calculateLoad());
    }

    /**
     * Check if load is trending upward
     */
    public boolean isLoadIncreasing() {
        if (historicalMetrics == null || historicalMetrics.size() < 5) {
            return false;
        }

        // Compare last 5 metrics
        int size = historicalMetrics.size();
        List<LoadMetrics> recentMetrics = historicalMetrics.subList(Math.max(0, size - 5), size);

        double firstLoad = recentMetrics.get(0).calculateLoadScore();
        double lastLoad = recentMetrics.get(recentMetrics.size() - 1).calculateLoadScore();

        return lastLoad > firstLoad * 1.1; // 10% increase threshold
    }

    /**
     * Check if response time is acceptable
     */
    public boolean hasAcceptableResponseTime(long maxResponseTimeMs) {
        return responseTime != null && responseTime <= maxResponseTimeMs;
    }

    /**
     * Get current capacity (0-100%)
     */
    public double getAvailableCapacity() {
        return Math.max(0, 100.0 - calculateLoad());
    }

    /**
     * Check if circuit breaker should trip
     */
    public boolean shouldTripCircuitBreaker(double errorThreshold, int minRequests) {
        return activeRequests >= minRequests && errorRate >= errorThreshold;
    }

    /**
     * Update current load metrics
     */
    public void updateMetrics(double cpuUsage, double memoryUsage, int activeRequests,
                              int queueDepth, long responseTime, double errorRate) {
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.activeRequests = activeRequests;
        this.queueDepth = queueDepth;
        this.responseTime = responseTime;
        this.errorRate = errorRate;
        this.timestamp = Instant.now();

        // Add to historical metrics
        LoadMetrics metrics = LoadMetrics.snapshot(
            serviceId, serviceName, cpuUsage, memoryUsage,
            activeRequests, queueDepth, responseTime, errorRate
        );
        addMetricsSnapshot(metrics);
    }

    /**
     * Calculate utilization percentage
     */
    public void calculateUtilization() {
        if (activeSteps != null && totalSteps != null && totalSteps > 0) {
            this.utilizationPercentage = ((double) activeSteps / totalSteps) * 100;
        }
    }

    /**
     * Create initial system load
     */
    public static SystemLoad create(String serviceId, String serviceName, String workflowInstanceId) {
        return SystemLoad.builder()
            .id(generateId())
            .serviceId(serviceId)
            .serviceName(serviceName)
            .workflowInstanceId(workflowInstanceId)
            .cpuUsage(0.0)
            .memoryUsage(0.0)
            .activeRequests(0)
            .queueDepth(0)
            .responseTime(0L)
            .errorRate(0.0)
            .activeSteps(0)
            .totalSteps(0)
            .utilizationPercentage(0.0)
            .timestamp(Instant.now())
            .historicalMetrics(new ArrayList<>())
            .targetUtilization(85.0)
            .criticalThreshold(95.0)
            .build();
    }

    private static String generateId() {
        return "load-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 10000);
    }
}
