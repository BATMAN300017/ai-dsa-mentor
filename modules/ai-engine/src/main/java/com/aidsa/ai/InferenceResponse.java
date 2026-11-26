package com.aidsa.ai;

/**
 * Structured output returned by a local inference runtime.
 */
public record InferenceResponse(
        String text,
        String modelName,
        long latencyMillis
) {
}
