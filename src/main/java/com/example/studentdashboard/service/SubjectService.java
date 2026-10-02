package com.example.studentdashboard.service;

import com.example.studentdashboard.dto.request.SubjectRequest;
import com.example.studentdashboard.dto.response.SubjectOverviewResponse;
import com.example.studentdashboard.dto.response.SubjectSummaryResponse;

import java.util.List;

public interface SubjectService {

    List<SubjectOverviewResponse> listSubjects(Long sessionId, Long termId);

    SubjectSummaryResponse createSubject(SubjectRequest request);

    SubjectSummaryResponse updateSubject(Long subjectId, SubjectRequest request);

    void deleteSubject(Long subjectId);
}
