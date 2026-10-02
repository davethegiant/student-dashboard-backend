package com.example.studentdashboard.exception;

/** Maps to 409 — e.g. duplicate email/username, or two teachers colliding on the same login email. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
