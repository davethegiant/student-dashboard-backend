package com.example.studentdashboard.service;

import com.example.studentdashboard.dto.request.ResultRequest;
import com.example.studentdashboard.dto.response.PerformanceRecordResponse;

import java.util.List;

public interface ResultService {

    List<PerformanceRecordResponse> listResults(Long classId, Long subjectId, Long sessionId, Long termId,
                                                 Integer limit, List<Long> teacherClassIds);

    List<PerformanceRecordResponse> listResultsForStudent(Long studentId, Long sessionId, Long termId, List<Long> teacherClassIds);

    /** Upserts by (student, subject, session, term) — resubmitting the same combination updates it. */
    PerformanceRecordResponse submitResult(ResultRequest request, List<Long> teacherClassIds);

    PerformanceRecordResponse updateResult(Long resultId, ResultRequest request, List<Long> teacherClassIds);

    void deleteResult(Long resultId, List<Long> teacherClassIds);
}
