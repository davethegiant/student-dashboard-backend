package com.example.studentdashboard.dto.response;

public record UserSummaryResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String role,
        Long teacherId
) {
}
