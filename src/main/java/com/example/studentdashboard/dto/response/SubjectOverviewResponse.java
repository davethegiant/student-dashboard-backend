package com.example.studentdashboard.dto.response;

public record SubjectOverviewResponse(
        Long id,
        String subjectCode,
        String name,
        double avgScore,
        int classCount,
        int teacherCount
) {
}
