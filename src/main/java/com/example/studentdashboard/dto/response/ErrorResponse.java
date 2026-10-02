package com.example.studentdashboard.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Shared shape for every error response the API returns — from
 * GlobalExceptionHandler as well as the security entry points below, so a
 * 401 from a missing token looks identical in structure to a 404 from a
 * missing student.
 */
public record ErrorResponse(
        int status,
        String message,
        Instant timestamp,
        List<String> details
) {
    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, Instant.now(), null);
    }

    public static ErrorResponse of(int status, String message, List<String> details) {
        return new ErrorResponse(status, message, Instant.now(), details);
    }
}
