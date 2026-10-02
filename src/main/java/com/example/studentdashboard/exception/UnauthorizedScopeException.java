package com.example.studentdashboard.exception;

/**
 * Maps to 403 — thrown when a TEACHER requests a class/student/report
 * outside their own assignments. This is enforced here in the service
 * layer (not just hidden in the frontend UI), matching the same
 * server-side scoping the mock layer already implements.
 */
public class UnauthorizedScopeException extends RuntimeException {
    public UnauthorizedScopeException(String message) {
        super(message);
    }
}
