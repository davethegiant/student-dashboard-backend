package com.example.studentdashboard.dto.response;

public record CommentResponse(
        Long id,
        String teacherName,
        String term,
        String comment,
        String date
) {
}
