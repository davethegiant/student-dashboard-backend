package com.example.studentdashboard.dto.request;

import com.example.studentdashboard.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** ADMIN-only — creates a raw login account. See TeacherController for the richer "add teacher + optional login" flow. */
public record RegisterUserRequest(
        @NotBlank @Size(min = 3, max = 60) String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
        @NotBlank String fullName,
        @NotNull Role role
) {
}
