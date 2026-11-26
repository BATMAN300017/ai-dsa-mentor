package com.aidsa.dto;

/**
 * Response payload returned by {@code POST /api/v1/ai/infer}.
 */
public record AiInferResponse(
        String text,
        String modelName,
        long latencyMillis,
        Integer promptTokens,
        Integer completionTokens,
        String finishReason
) {
}
