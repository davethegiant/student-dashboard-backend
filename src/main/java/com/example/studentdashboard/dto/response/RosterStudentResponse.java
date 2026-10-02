package com.example.studentdashboard.dto.response;

/** Lightweight shape for GET /api/classes/{id}/roster — attendance marking only needs id + name, not risk/averages. */
public record RosterStudentResponse(
        Long id,
        String studentCode,
        String name
) {
}
