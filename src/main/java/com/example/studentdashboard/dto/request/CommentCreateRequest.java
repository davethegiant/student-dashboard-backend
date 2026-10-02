package com.example.studentdashboard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentCreateRequest(
        @NotNull(message = "Academic session is required") Long academicSessionId,
        @NotNull(message = "Term is required") Long termId,
        @NotBlank(message = "Comment cannot be empty") String comment
) {
}
