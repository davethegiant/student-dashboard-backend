package com.example.studentdashboard.dto.response;

public record AttendanceBreakdownResponse(
        int present,
        int absent,
        int late,
        int total
) {
}
