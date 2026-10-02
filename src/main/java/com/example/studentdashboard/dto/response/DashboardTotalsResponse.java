package com.example.studentdashboard.dto.response;

public record DashboardTotalsResponse(
        long totalStudents,
        long totalTeachers,
        long totalClasses,
        double avgPerformance,
        double avgAttendance,
        long atRisk
) {
}
