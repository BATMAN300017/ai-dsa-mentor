package com.aidsa.ai;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Configuration for discovering and launching the local loopback {@code llama-server} runtime.
 */
public record LlamaRuntimeConfig(
        String configuredExecutablePath,
        Path appDataRuntimeDir,
        Path workspaceRuntimeDir,
        Path logsDir,
        int defaultContextSize,
        int defaultThreads,
        int defaultGpuLayers,
        int preferredPort,
        Duration startupTimeout,
        Duration requestTimeout
) {
    public static int recommendedCpuThreads() {
        int logical = Runtime.getRuntime().availableProcessors();
        return Math.max(1, Math.min(8, Math.max(1, logical / 2)));
    }

    public static LlamaRuntimeConfig defaults(Path appDataRuntimeDir, Path workspaceRuntimeDir, Path logsDir) {
        return new LlamaRuntimeConfig(
                "",
                appDataRuntimeDir,
                workspaceRuntimeDir,
                logsDir,
                4096,
                recommendedCpuThreads(),
                0,
                0,
                Duration.ofSeconds(90),
                Duration.ofSeconds(120)
        );
    }
}
