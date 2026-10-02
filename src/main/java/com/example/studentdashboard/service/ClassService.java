package com.example.studentdashboard.service;

import com.example.studentdashboard.dto.request.ClassRequest;
import com.example.studentdashboard.dto.response.ClassDetailResponse;
import com.example.studentdashboard.dto.response.ClassOverviewResponse;
import com.example.studentdashboard.dto.response.ClassSummaryResponse;
import com.example.studentdashboard.dto.response.RosterStudentResponse;

import java.util.List;

public interface ClassService {

    List<ClassOverviewResponse> listClasses(Long sessionId, Long termId, List<Long> teacherClassIds);

    ClassDetailResponse getClassDetail(Long classId, Long sessionId, Long termId, List<Long> teacherClassIds);

    List<RosterStudentResponse> getRoster(Long classId);

    ClassSummaryResponse createClass(ClassRequest request);

    ClassSummaryResponse updateClass(Long classId, ClassRequest request);

    void deleteClass(Long classId);
}
