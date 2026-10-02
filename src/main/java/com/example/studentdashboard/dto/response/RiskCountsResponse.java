package com.example.studentdashboard.dto.response;

public record RiskCountsResponse(
        long low,
        long medium,
        long high
) {
}
