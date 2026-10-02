package com.example.studentdashboard.dto.response;

public record TopPerformerResponse(
        Long id,
        String studentCode,
        String name,
        double avg
) {
}
