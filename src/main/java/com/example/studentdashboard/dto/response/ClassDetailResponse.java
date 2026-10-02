package com.example.studentdashboard.dto.response;

import java.util.List;

public record ClassDetailResponse(
        Long id,
        String classCode,
        String name,
        String level,
        String formTeacherName,
        int studentCount,
        double avgPerformance,
        double avgAttendance,
        TopPerformerResponse topPerformer,
        int atRisk,
        List<StudentSummaryResponse> students
) {
}
