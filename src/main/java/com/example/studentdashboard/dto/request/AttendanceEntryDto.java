package com.example.studentdashboard.dto.request;

import com.example.studentdashboard.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record AttendanceEntryDto(
        @NotNull Long studentId,
        @NotNull AttendanceStatus status
) {
}
