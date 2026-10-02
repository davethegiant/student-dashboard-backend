package com.example.studentdashboard.dto.response;

import java.util.List;

public record StudentProfileResponse(
        Long id,
        String studentCode,
        String name,
        String gender,
        Long classId,
        String className,
        String status,
        double avgScore,
        double attendanceRate,
        int subjectCount,
        List<PerformanceEntryResponse> performance,
        List<ChartPointResponse> trend,
        AttendanceBreakdownResponse attendance,
        RiskInfo risk,
        List<CommentResponse> comments
) {
}
