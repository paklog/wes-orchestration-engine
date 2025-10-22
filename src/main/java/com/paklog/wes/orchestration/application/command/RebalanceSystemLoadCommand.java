package com.paklog.wes.orchestration.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Command to rebalance system load
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebalanceSystemLoadCommand {

    /**
     * Target system utilization (default 0.85 = 85%)
     */
    @Builder.Default
    private Double targetUtilization = 0.85;
}
