package com.aidsa.ai;

import java.time.Duration;
import java.util.List;

/**
 * Generation parameter overrides for a local inference request.
 */
public record InferenceParameters(
        double topP,
        double repeatPenalty,
        List<String> stopSequences,
        Duration requestTimeout
) {
    public InferenceParameters {
        if (topP <= 0.0 || topP > 1.0) {
            throw new IllegalArgumentException("topP must be in (0.0, 1.0]");
        }
        if (repeatPenalty < 0.5 || repeatPenalty > 2.5) {
            throw new IllegalArgumentException("repeatPenalty must be in [0.5, 2.5]");
        }
        stopSequences = stopSequences == null ? List.of() : List.copyOf(stopSequences);
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(120) : requestTimeout;
    }

    public static InferenceParameters defaults() {
        return new InferenceParameters(0.95, 1.05, List.of(), Duration.ofSeconds(120));
    }
}
