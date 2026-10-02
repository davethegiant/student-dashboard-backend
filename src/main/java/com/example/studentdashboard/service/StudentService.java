package com.example.studentdashboard.service;

import com.example.studentdashboard.dto.request.CommentCreateRequest;
import com.example.studentdashboard.dto.request.StudentRequest;
import com.example.studentdashboard.dto.response.CommentResponse;
import com.example.studentdashboard.dto.response.StudentProfileResponse;
import com.example.studentdashboard.dto.response.StudentSummaryResponse;
import com.example.studentdashboard.entity.Gender;
import com.example.studentdashboard.entity.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StudentService {

    /**
     * teacherClassIds: null for ADMIN (no restriction), or the caller's
     * assigned class IDs for TEACHER — enforced here, not just hidden in
     * the frontend UI, mirroring the same principle as report scoping.
     */
    Page<StudentSummaryResponse> listStudents(String search, Long classId, Gender gender, RecordStatus status,
                                               String riskLevel, Long sessionId, Long termId,
                                               List<Long> teacherClassIds, Pageable pageable);

    StudentProfileResponse getProfile(Long studentId, Long sessionId, Long termId, List<Long> teacherClassIds);

    StudentSummaryResponse createStudent(StudentRequest request);

    StudentSummaryResponse updateStudent(Long studentId, StudentRequest request);

    void updateStatus(Long studentId, RecordStatus status);

    void deleteStudent(Long studentId);

    CommentResponse addComment(Long studentId, CommentCreateRequest request, Long teacherId);
}
