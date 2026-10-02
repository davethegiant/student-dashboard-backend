package com.example.studentdashboard.dto.request;

import com.example.studentdashboard.entity.RecordStatus;
import jakarta.validation.constraints.NotNull;

/** Shared by the student and teacher PATCH .../status endpoints. */
public record StatusUpdateRequest(
        @NotNull(message = "Status is required") RecordStatus status
) {
}
