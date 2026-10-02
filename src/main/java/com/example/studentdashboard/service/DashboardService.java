package com.example.studentdashboard.service;

import com.example.studentdashboard.dto.response.AdminDashboardResponse;
import com.example.studentdashboard.dto.response.TeacherDashboardResponse;

public interface DashboardService {

    AdminDashboardResponse getAdminDashboard(Long sessionId, Long termId, Long classId);

    TeacherDashboardResponse getTeacherDashboard(Long teacherId, Long sessionId, Long termId);
}
