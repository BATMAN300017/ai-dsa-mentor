package com.aidsa.dto;

/**
 * Ephemeral UI session token issued to verified local frontend origins.
 */
public record SecurityHandshakeResponse(
        String headerName,
        String token
) {
}
