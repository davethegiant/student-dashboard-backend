package com.example.studentdashboard.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Submits (or upserts, per the unique constraint on Result) a CA + Exam
 * score. class_id is derived server-side from the student's current class
 * rather than trusted from the client, to prevent an inconsistent record.
 */
public record ResultRequest(
        @NotNull(message = "Student is required") Long studentId,
        @NotNull(message = "Subject is required") Long subjectId,
        @NotNull(message = "Academic session is required") Long academicSessionId,
        @NotNull(message = "Term is required") Long termId,
        @NotNull(message = "CA score is required")
        @Min(value = 0, message = "CA score cannot be negative")
        @Max(value = 40, message = "CA score cannot exceed 40") Integer ca,
        @NotNull(message = "Exam score is required")
        @Min(value = 0, message = "Exam score cannot be negative")
        @Max(value = 60, message = "Exam score cannot exceed 60") Integer exam,
        String comment
) {
}
