package com.example.studentdashboard.dto.response;

public record LoginResponse(
        String token,
        UserSummaryResponse user
) {
}
