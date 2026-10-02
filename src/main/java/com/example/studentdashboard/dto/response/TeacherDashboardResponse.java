package com.example.studentdashboard.dto.response;

import java.util.List;

public record TeacherDashboardResponse(
        TeacherDashboardTotalsResponse totals,
        List<ChartPointResponse> classPerformance,
        List<ChartPointResponse> attendanceTrend,
        List<ChartPointResponse> subjectPerformance,
        List<StudentSummaryResponse> needsAttention
) {
}
