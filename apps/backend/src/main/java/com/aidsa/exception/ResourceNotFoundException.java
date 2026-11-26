package com.aidsa.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested entity or endpoint resource does not exist.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
