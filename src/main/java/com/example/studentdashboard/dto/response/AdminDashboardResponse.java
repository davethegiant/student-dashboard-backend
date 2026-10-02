package com.example.studentdashboard.dto.response;

import java.util.List;

public record AdminDashboardResponse(
        DashboardTotalsResponse totals,
        List<ChartPointResponse> performanceByClass,
        AttendanceOverviewResponse attendanceOverview,
        List<ChartPointResponse> performanceTrend,
        List<ChartPointResponse> attendanceTrend,
        List<StudentSummaryResponse> topPerformers,
        List<StudentSummaryResponse> needsAttention,
        List<ActivityResponse> recentActivity
) {
}
