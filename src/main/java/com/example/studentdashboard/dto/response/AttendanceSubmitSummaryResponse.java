package com.example.studentdashboard.dto.response;

/** Returned right after a class attendance submission — powers the "Attendance Summary" card. */
public record AttendanceSubmitSummaryResponse(
        int present,
        int absent,
        int late,
        int total,
        double rate
) {
}
