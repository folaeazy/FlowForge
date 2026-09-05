package com.flowforge.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CreateJobRequest(
        @NotBlank String tenantId,
        @NotBlank String type,
        Map<String, Object> payload
) { }
