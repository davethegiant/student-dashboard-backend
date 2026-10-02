package com.example.studentdashboard.dto.response;

import java.util.List;

public record AnalyticsResponse(
        List<ChartPointResponse> classComparison,
        List<ChartPointResponse> subjectComparison,
        List<ChartPointResponse> performanceTrends,
        List<ChartPointResponse> attendanceTrends,
        RiskCountsResponse riskCounts
) {
}
