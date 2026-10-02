package com.example.studentdashboard.dto.response;

public record TeacherDashboardTotalsResponse(
        int myClasses,
        long myStudents,
        double avgPerformance,
        double avgAttendance,
        long atRisk
) {
}
