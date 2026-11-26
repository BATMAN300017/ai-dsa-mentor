package com.aidsa.ai;

/**
 * Thrown when inference is requested before local llama.cpp runtime and GGUF weights are configured.
 */
public class ModelRuntimeNotConfiguredException extends IllegalStateException {

    public ModelRuntimeNotConfiguredException(String message) {
        super(message);
    }
}
