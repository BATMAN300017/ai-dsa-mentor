package com.aidsa.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Validated request payload for {@code POST /api/v1/ai/infer}.
 */
public record AiInferRequest(
        @Size(max = 16000) String systemPrompt,
        @NotBlank @Size(max = 32000) String userPrompt,
        @Min(1) @Max(4096) Integer maxTokens,
        @DecimalMin("0.0") @DecimalMax("2.0") Double temperature,
        @DecimalMin("0.05") @DecimalMax("1.0") Double topP
) {
}
