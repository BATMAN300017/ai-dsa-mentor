package com.aidsa.ai;

/**
 * Operational states of the local inference engine (llama.cpp planned for Task 2+).
 */
public enum InferenceRuntimeStatus {
    UNINITIALIZED,
    RUNTIME_MISSING,
    MODEL_NOT_LOADED,
    READY
}
