package com.example.studentdashboard.dto.request;

import com.example.studentdashboard.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record AttendanceUpdateRequest(
        @NotNull(message = "Status is required") AttendanceStatus status,
        String remarks
) {
}
