package com.example.studentdashboard.dto.request;

import com.example.studentdashboard.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Used for both create (POST) and full update (PUT) — matches the frontend's Student form exactly (no email field). */
public record StudentRequest(
        @NotBlank(message = "Student name is required") String name,
        @NotNull(message = "Gender is required") Gender gender,
        @NotNull(message = "Class is required") Long classId,
        LocalDate dob,
        String guardianName,
        String guardianPhone,
        String address,
        LocalDate admittedDate
) {
}
