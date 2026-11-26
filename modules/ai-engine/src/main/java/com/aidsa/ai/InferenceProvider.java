package com.aidsa.ai;

/**
 * Contract for local model inference providers (e.g., llama.cpp running Qwen2.5-Coder-3B-Instruct).
 * Concrete GGUF inference will be implemented in a subsequent task.
 */
public interface InferenceProvider {

    InferenceRuntimeStatus status();

    boolean isModelLoaded();

    InferenceResponse generate(InferenceRequest request);
}
