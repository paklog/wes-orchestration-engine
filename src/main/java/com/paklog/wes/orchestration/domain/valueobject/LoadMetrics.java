package com.paklog.wes.orchestration.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * System load metrics for load balancing decisions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadMetrics implements Serializable {

    private String serviceId;
    private String serviceName;

    private Double cpuUsagePercent;
    private Double memoryUsagePercent;
    private Integer activeRequests;
    private Integer queueDepth;
    private Long averageResponseTimeMs;
    private Double errorRate;

    private Instant timestamp;

    @Builder.Default
    private Double targetUtilization = 85.0;

    @Builder.Default
    private Double criticalThreshold = 95.0;

    /**
     * Calculate overall load score (0-100)
     */
    public double calculateLoadScore() {
        double cpuScore = cpuUsagePercent != null ? cpuUsagePercent : 0.0;
        double memoryScore = memoryUsagePercent != null ? memoryUsagePercent : 0.0;
        double queueScore = calculateQueueScore();
        double errorScore = calculateErrorScore();

        // Weighted average
        return (cpuScore * 0.3) + (memoryScore * 0.3) + (queueScore * 0.2) + (errorScore * 0.2);
    }

    /**
     * Check if service is overloaded
     */
    public boolean isOverloaded() {
        return calculateLoadScore() >= criticalThreshold;
    }

    /**
     * Check if service needs rebalancing
     */
    public boolean needsRebalancing() {
        return calculateLoadScore() >= targetUtilization;
    }

    /**
     * Check if service can accept more work
     */
    public boolean canAcceptWork() {
        return calculateLoadScore() < targetUtilization && errorRate < 0.5;
    }

    /**
     * Calculate queue depth score (0-100)
     */
    private double calculateQueueScore() {
        if (queueDepth == null || queueDepth == 0) {
            return 0.0;
        }
        // Normalize queue depth to 0-100 scale (assume max queue of 1000)
        return Math.min((queueDepth / 1000.0) * 100, 100.0);
    }

    /**
     * Calculate error rate score (0-100)
     */
    private double calculateErrorScore() {
        if (errorRate == null) {
            return 0.0;
        }
        // Error rate is already 0-1, convert to 0-100
        return errorRate * 100;
    }

    /**
     * Check if response time is acceptable
     */
    public boolean hasAcceptableResponseTime(long maxResponseTimeMs) {
        return averageResponseTimeMs != null && averageResponseTimeMs <= maxResponseTimeMs;
    }

    /**
     * Create metrics snapshot
     */
    public static LoadMetrics snapshot(String serviceId, String serviceName,
                                       double cpuUsage, double memoryUsage,
                                       int activeRequests, int queueDepth,
                                       long avgResponseTime, double errorRate) {
        return LoadMetrics.builder()
            .serviceId(serviceId)
            .serviceName(serviceName)
            .cpuUsagePercent(cpuUsage)
            .memoryUsagePercent(memoryUsage)
            .activeRequests(activeRequests)
            .queueDepth(queueDepth)
            .averageResponseTimeMs(avgResponseTime)
            .errorRate(errorRate)
            .timestamp(Instant.now())
            .build();
    }
}
