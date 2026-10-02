package com.example.studentdashboard.service;

import com.example.studentdashboard.dto.request.TeacherCreateRequest;
import com.example.studentdashboard.dto.request.TeacherLoginRequest;
import com.example.studentdashboard.dto.request.TeacherRequest;
import com.example.studentdashboard.dto.response.TeacherSummaryResponse;
import com.example.studentdashboard.entity.RecordStatus;

import java.util.List;

public interface TeacherService {

    List<TeacherSummaryResponse> listTeachers(String search, RecordStatus status);

    TeacherSummaryResponse createTeacher(TeacherCreateRequest request);

    TeacherSummaryResponse updateTeacher(Long teacherId, TeacherRequest request);

    void updateStatus(Long teacherId, RecordStatus status);

    void deleteTeacher(Long teacherId);

    /** Grants login access to a teacher who has none, or resets an existing one — same operation either way. */
    TeacherSummaryResponse grantOrResetLogin(Long teacherId, TeacherLoginRequest request);

    void revokeLogin(Long teacherId);
}
