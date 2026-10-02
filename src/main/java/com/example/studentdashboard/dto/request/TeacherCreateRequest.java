package com.example.studentdashboard.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

/**
 * Wraps TeacherRequest plus an optional login grant at creation time —
 * mirrors the frontend's "Add Teacher" modal, which has a
 * "Grant login access now" checkbox alongside the profile fields.
 */
public record TeacherCreateRequest(
        @Valid TeacherRequest profile,
        boolean grantLogin,
        String loginUsername,
        @Size(min = 8, message = "Password must be at least 8 characters") String loginPassword
) {
}
