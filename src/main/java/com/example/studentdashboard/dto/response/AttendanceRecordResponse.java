package com.example.studentdashboard.dto.response;

/** Row shape for GET /api/attendance and GET /api/attendance/student/{id}. */
public record AttendanceRecordResponse(
        Long id,
        String studentName,
        String className,
        String date,
        String status,
        String remarks
) {
}
