package com.example.studentdashboard.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ClassRequest(
        @NotBlank(message = "Class code is required") String classCode,
        @NotBlank(message = "Class name is required") String name,
        @NotBlank(message = "Level is required") String level,
        /** Optional — null clears the form teacher. Must be one of the teachers actually assigned to this class (validated in ClassService). */
        Long formTeacherId
) {
}
