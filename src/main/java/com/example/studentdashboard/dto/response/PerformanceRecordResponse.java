package com.example.studentdashboard.dto.response;

/** Row shape for GET /api/results (the "Recently Recorded Scores" table). */
public record PerformanceRecordResponse(
        Long id,
        String studentName,
        String className,
        String subjectName,
        int ca,
        int exam,
        int total,
        String grade
) {
}
