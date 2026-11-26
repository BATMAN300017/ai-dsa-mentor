package com.aidsa.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Validated request parameters for {@code POST /api/v1/model/load}.
 */
public record ModelLoadRequest(
        @Min(512) @Max(32768) Integer contextSize,
        @Min(1) @Max(64) Integer threads,
        @Min(0) @Max(128) Integer gpuLayers
) {
}
