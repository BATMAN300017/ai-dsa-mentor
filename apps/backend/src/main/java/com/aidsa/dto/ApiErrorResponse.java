package com.aidsa.dto;

import java.time.Instant;

/**
 * Standardized JSON error payload returned by {@link com.aidsa.exception.GlobalExceptionHandler}.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
