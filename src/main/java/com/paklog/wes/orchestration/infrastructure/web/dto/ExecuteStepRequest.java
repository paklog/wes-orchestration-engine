package com.paklog.wes.orchestration.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteStepRequest {

    @NotBlank(message = "Step ID is required")
    private String stepId;

    @NotNull(message = "Input data is required")
    private Map<String, Object> inputData;
}
