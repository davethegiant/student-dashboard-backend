package com.example.studentdashboard.dto.response;

public record ActivityResponse(
        Long id,
        String type,
        String message,
        String actor,
        String time
) {
}
