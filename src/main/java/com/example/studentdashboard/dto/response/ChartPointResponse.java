package com.example.studentdashboard.dto.response;

/** Generic label/value pair — backs every bar/line chart on the dashboard and analytics pages. */
public record ChartPointResponse(
        String label,
        double value
) {
}
