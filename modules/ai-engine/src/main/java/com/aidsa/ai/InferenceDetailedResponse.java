package com.aidsa.ai;

/**
 * Detailed completion result returned by {@link LlamaCppInferenceProvider}, preserving
 * compatibility with {@link InferenceResponse}.
 */
public record InferenceDetailedResponse(
        String text,
        String modelName,
        long latencyMillis,
        Integer promptTokens,
        Integer completionTokens,
        String finishReason
) {
    public InferenceResponse toInferenceResponse() {
        return new InferenceResponse(text, modelName, latencyMillis);
    }
}
