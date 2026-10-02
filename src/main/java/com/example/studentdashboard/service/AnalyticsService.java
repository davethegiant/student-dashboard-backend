package com.example.studentdashboard.service;

import com.example.studentdashboard.dto.response.AnalyticsResponse;
import com.example.studentdashboard.dto.response.StudentSummaryResponse;

import java.util.List;

public interface AnalyticsService {

    /**
     * teacherClassIds: null for ADMIN (school-wide). For a TEACHER caller,
     * every chart here is scoped to their own classes — including
     * subjectComparison and the trend charts, which the original mock this
     * was ported from computed school-wide regardless of role. That's
     * corrected here rather than carried forward, for consistency with
     * every other scoping rule enforced in this backend.
     */
    AnalyticsResponse getAnalytics(Long sessionId, Long termId, List<Long> teacherClassIds);

    List<StudentSummaryResponse> getStudentsAtRisk(Long sessionId, Long termId, List<Long> teacherClassIds);
}
