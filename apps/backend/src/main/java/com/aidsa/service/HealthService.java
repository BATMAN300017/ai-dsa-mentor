package com.aidsa.service;

import com.aidsa.ai.InferenceProvider;
import com.aidsa.config.AppProperties;
import com.aidsa.dto.HealthResponse;
import org.springframework.stereotype.Service;

/**
 * Provides application health telemetry reflecting live local model readiness.
 */
@Service
public class HealthService {

    private final AppProperties properties;
    private final InferenceProvider inferenceProvider;

    public HealthService(AppProperties properties, InferenceProvider inferenceProvider) {
        this.properties = properties;
        this.inferenceProvider = inferenceProvider;
    }

    public HealthResponse getHealth() {
        return new HealthResponse(
                "UP",
                properties.applicationName(),
                properties.version(),
                inferenceProvider.isModelLoaded()
        );
    }
}
