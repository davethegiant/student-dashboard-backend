package com.example.studentdashboard.dto.response;

/** Generic acknowledgement body for actions that don't return a resource (e.g. "Teacher deactivated"). */
public record MessageResponse(String message) {
}
