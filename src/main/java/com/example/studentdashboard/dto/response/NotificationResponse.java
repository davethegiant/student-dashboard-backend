package com.example.studentdashboard.dto.response;

public record NotificationResponse(
        Long id,
        String type,
        String message,
        String date,
        boolean read
) {
}
