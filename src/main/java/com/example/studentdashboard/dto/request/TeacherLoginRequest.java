package com.example.studentdashboard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Grants or resets a teacher's login access — POST /api/teachers/{id}/login */
public record TeacherLoginRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password
) {
}
