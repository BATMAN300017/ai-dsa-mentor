package com.aidsa.util;

import java.util.List;
import java.util.Locale;

/**
 * Helpers for verifying loopback-only hosts and explicitly allowlisted local origins.
 */
public final class LoopbackValidator {

    private LoopbackValidator() {
    }

    public static boolean isLoopbackHostHeader(String hostHeader) {
        if (hostHeader == null || hostHeader.isBlank()) {
            return true;
        }
        String normalized = hostHeader.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[::1]")) {
            return normalized.equals("[::1]") || normalized.startsWith("[::1]:");
        }
        String hostOnly = normalized;
        int colonIdx = normalized.indexOf(':');
        if (colonIdx > -1) {
            hostOnly = normalized.substring(0, colonIdx);
        }
        return "127.0.0.1".equals(hostOnly) || "localhost".equals(hostOnly);
    }

    public static boolean isAllowedOrigin(
            String origin,
            List<String> allowedOrigins,
            List<String> allowedExtensionIds
    ) {
        if (origin == null || origin.isBlank()) {
            return true;
        }
        String trimmed = origin.trim();
        if (allowedOrigins != null && allowedOrigins.contains(trimmed)) {
            return true;
        }
        if (allowedExtensionIds != null && trimmed.startsWith("chrome-extension://")) {
            String extId = trimmed.substring("chrome-extension://".length());
            return !extId.isBlank() && allowedExtensionIds.contains(extId);
        }
        return false;
    }
}
