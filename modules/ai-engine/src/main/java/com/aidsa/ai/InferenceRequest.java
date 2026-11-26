package com.aidsa.ai;

/**
 * Immutable local LLM generation request passed to an {@link InferenceProvider}.
 */
public record InferenceRequest(
        String systemPrompt,
        String userPrompt,
        int maxTokens,
        double temperature
) {
}
