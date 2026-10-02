package com.example.studentdashboard.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record TeacherRequest(
        @NotBlank(message = "Teacher name is required") String name,
        @NotBlank @Email(message = "A valid email is required") String email,
        String phone,
        LocalDate joinedDate,
        List<Long> classIds,
        List<Long> subjectIds
) {
}
