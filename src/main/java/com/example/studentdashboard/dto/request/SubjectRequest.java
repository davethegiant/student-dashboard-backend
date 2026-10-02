package com.example.studentdashboard.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SubjectRequest(
        @NotBlank(message = "Subject code is required") String subjectCode,
        @NotBlank(message = "Subject name is required") String name
) {
}
