package com.example.studentdashboard.dto.response;

public record AttendanceOverviewResponse(
        int present,
        int absent,
        int late
) {
}
