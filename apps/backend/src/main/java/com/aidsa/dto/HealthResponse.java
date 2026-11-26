package com.aidsa.dto;

/**
 * Health check response payload returned by {@code GET /api/v1/health}.
 */
public record HealthResponse(
        String status,
        String application,
        String version,
        boolean modelLoaded
) {
}
