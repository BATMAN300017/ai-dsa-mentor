package com.aidsa.service;

import com.aidsa.config.AppProperties;
import com.aidsa.dto.HealthResponse;
import org.springframework.stereotype.Service;

/**
 * Provides application health telemetry.
 * In Task 1, {@code modelLoaded} is always {@code false} because local GGUF inference
 * belongs to a subsequent task.
 */
@Service
public class HealthService {

    private final AppProperties properties;

    public HealthService(AppProperties properties) {
        this.properties = properties;
    }

    public HealthResponse getHealth() {
        return new HealthResponse(
                "UP",
                properties.applicationName(),
                properties.version(),
                false
        );
    }
}
