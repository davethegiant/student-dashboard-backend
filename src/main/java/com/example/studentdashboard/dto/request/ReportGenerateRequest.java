package com.example.studentdashboard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * type is one of: "student", "class", "attendance", "at_risk", "subject" —
 * matching the frontend's five report types exactly. classId/subjectId/
 * studentId are only required for the types that need them; validated in
 * ReportService rather than here, since which fields are required depends
 * on the type.
 */
public record ReportGenerateRequest(
        @NotBlank(message = "Report type is required") String type,
        Long classId,
        Long subjectId,
        Long studentId,
        @NotNull(message = "Academic session is required") Long academicSessionId,
        @NotNull(message = "Term is required") Long termId
) {
}
