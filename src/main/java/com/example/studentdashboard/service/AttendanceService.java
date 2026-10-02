package com.example.studentdashboard.service;

import com.example.studentdashboard.dto.request.AttendanceBulkSubmitRequest;
import com.example.studentdashboard.dto.request.AttendanceUpdateRequest;
import com.example.studentdashboard.dto.response.AttendanceRecordResponse;
import com.example.studentdashboard.dto.response.AttendanceSubmitSummaryResponse;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    List<AttendanceRecordResponse> listAttendance(Long classId, Long sessionId, Long termId, LocalDate date,
                                                    List<Long> teacherClassIds);

    List<AttendanceRecordResponse> listForStudent(Long studentId, Long sessionId, Long termId, List<Long> teacherClassIds);

    AttendanceSubmitSummaryResponse submitBulk(AttendanceBulkSubmitRequest request, List<Long> teacherClassIds);

    AttendanceRecordResponse updateAttendance(Long attendanceId, AttendanceUpdateRequest request, List<Long> teacherClassIds);

    void deleteAttendance(Long attendanceId, List<Long> teacherClassIds);
}
