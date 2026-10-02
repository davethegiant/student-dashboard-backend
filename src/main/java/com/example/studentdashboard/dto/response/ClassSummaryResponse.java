package com.example.studentdashboard.dto.response;

public record ClassSummaryResponse(
        Long id,
        String classCode,
        String name,
        String level
) {
}
