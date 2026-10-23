package com.frauddetection.util;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Standardized error response structure across all REST APIs.
 */
public record ApiErrorResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path);
    }
}
