package com.aidsa.ai;

/**
 * Operational states of the local inference engine backed by llama.cpp.
 */
public enum InferenceRuntimeStatus {
    UNINITIALIZED,
    RUNTIME_MISSING,
    MODEL_NOT_LOADED,
    STARTING,
    READY,
    UNLOADING,
    ERROR
}
