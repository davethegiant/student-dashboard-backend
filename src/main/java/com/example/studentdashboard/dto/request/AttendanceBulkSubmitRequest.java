package com.example.studentdashboard.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/** Whole-class roster submission — mirrors the frontend's "Mark All Present" + per-student override flow. */
public record AttendanceBulkSubmitRequest(
        @NotNull(message = "Class is required") Long classId,
        @NotNull(message = "Date is required") LocalDate date,
        @NotNull(message = "Academic session is required") Long academicSessionId,
        @NotNull(message = "Term is required") Long termId,
        @NotEmpty(message = "At least one attendance entry is required") @Valid List<AttendanceEntryDto> entries
) {
}
