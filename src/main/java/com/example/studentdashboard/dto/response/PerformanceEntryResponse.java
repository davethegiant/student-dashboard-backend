package com.example.studentdashboard.dto.response;

/** One subject's result — nested inside StudentProfileResponse.performance. */
public record PerformanceEntryResponse(
        Long id,
        String subjectName,
        int ca,
        int exam,
        int total,
        String grade,
        String comment
) {
}
