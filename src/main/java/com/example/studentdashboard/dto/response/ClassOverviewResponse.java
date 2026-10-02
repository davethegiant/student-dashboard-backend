package com.example.studentdashboard.dto.response;

public record ClassOverviewResponse(
        Long id,
        String classCode,
        String name,
        String level,
        String formTeacherName,
        int studentCount,
        double avgPerformance,
        double avgAttendance,
        int atRisk
) {
}
