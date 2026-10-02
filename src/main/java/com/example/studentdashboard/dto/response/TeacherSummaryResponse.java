package com.example.studentdashboard.dto.response;

import java.util.List;

public record TeacherSummaryResponse(
        Long id,
        String teacherCode,
        String name,
        String email,
        String phone,
        String status,
        String joinedDate,
        List<String> classNames,
        List<String> subjectNames,
        boolean hasLogin
) {
}
