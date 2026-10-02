package com.example.studentdashboard.service;

import com.example.studentdashboard.dto.request.ReportGenerateRequest;
import com.example.studentdashboard.dto.response.ReportResponse;

import java.util.List;

public interface ReportService {

    /**
     * teacherClassIds: null for ADMIN (unrestricted). For a TEACHER, every
     * report type is authorized server-side — requesting a class/student
     * outside their assignment returns a "not authorized" report body
     * rather than the data, rather than relying on the frontend to hide
     * the option.
     */
    ReportResponse generateReport(ReportGenerateRequest request, List<Long> teacherClassIds);
}
