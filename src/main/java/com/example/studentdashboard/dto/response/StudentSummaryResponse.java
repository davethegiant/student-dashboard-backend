package com.example.studentdashboard.dto.response;

public record StudentSummaryResponse(
        Long id,
        String studentCode,
        String name,
        String gender,
        Long classId,
        String className,
        String status,
        double avgScore,
        double attendanceRate,
        RiskInfo risk
) {
}
