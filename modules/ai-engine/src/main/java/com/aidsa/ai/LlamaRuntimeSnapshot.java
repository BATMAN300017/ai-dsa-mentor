package com.aidsa.ai;

/**
 * Immutable status snapshot of the local {@code llama.cpp} runtime process and loaded model.
 */
public record LlamaRuntimeSnapshot(
        InferenceRuntimeStatus status,
        boolean runtimeAvailable,
        String runtimeSource,
        String executableName,
        boolean modelLoaded,
        boolean modelLoading,
        int port,
        int contextSize,
        int threads,
        int gpuLayers,
        String loadedModelName,
        String lastError,
        Long pid
) {
}
