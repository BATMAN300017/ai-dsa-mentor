package com.aidsa.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when model configuration metadata is invalid or unavailable.
 */
public class ModelConfigurationException extends ApiException {

    public ModelConfigurationException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
