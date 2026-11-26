package com.aidsa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Strongly-typed configuration properties for AI DSA Mentor backend foundation.
 */
@ConfigurationProperties(prefix = "aidsa")
public record AppProperties(
        String applicationName,
        String version,
        ModelConfig model,
        RuntimeConfig runtime,
        FrontendConfig frontend,
        SecurityConfig security
) {
    public record ModelConfig(
            String slug,
            String descriptorPath,
            String appDataDir
    ) {
    }

    public record RuntimeConfig(
            String executablePath,
            String appDataDir,
            int contextSize,
            int threads,
            int gpuLayers,
            int preferredPort,
            int startupTimeoutSeconds,
            int requestTimeoutSeconds
    ) {
    }

    public record FrontendConfig(
            String distPath
    ) {
    }

    public record SecurityConfig(
            List<String> allowedOrigins,
            List<String> allowedExtensionIds,
            boolean requireLocalTokenForMutations
    ) {
    }
}
