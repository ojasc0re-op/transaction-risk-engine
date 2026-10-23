package com.frauddetection.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
    public ResourceNotFoundException(String resource, String identifier) {
        super(resource + " not found with identifier: " + identifier);
    }
}
