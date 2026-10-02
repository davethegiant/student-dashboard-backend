package com.example.studentdashboard.service.impl;

import com.example.studentdashboard.dto.request.AttendanceBulkSubmitRequest;
import com.example.studentdashboard.dto.request.AttendanceEntryDto;
import com.example.studentdashboard.dto.request.AttendanceUpdateRequest;
import com.example.studentdashboard.dto.response.AttendanceRecordResponse;
import com.example.studentdashboard.dto.response.AttendanceSubmitSummaryResponse;
import com.example.studentdashboard.entity.AcademicSession;
import com.example.studentdashboard.entity.Attendance;
import com.example.studentdashboard.entity.SchoolClass;
import com.example.studentdashboard.entity.Student;
import com.example.studentdashboard.entity.Term;
import com.example.studentdashboard.exception.ResourceNotFoundException;
import com.example.studentdashboard.exception.UnauthorizedScopeException;
import com.example.studentdashboard.repository.AttendanceRepository;
import com.example.studentdashboard.repository.SchoolClassRepository;
import com.example.studentdashboard.repository.StudentRepository;
import com.example.studentdashboard.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AcademicPeriodResolver periodResolver;

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> listAttendance(Long classId, Long sessionId, Long termId, LocalDate date,
                                                           List<Long> teacherClassIds) {
        AcademicSession session = periodResolver.resolveSession(sessionId);
        Term term = periodResolver.resolveTerm(termId);

        List<Attendance> records;
        if (classId != null) {
            if (teacherClassIds != null && !teacherClassIds.contains(classId)) {
                throw new UnauthorizedScopeException("You can only view attendance for classes you teach.");
            }
            records = date != null
                    ? attendanceRepository.findBySchoolClassIdAndDate(classId, date)
                    : attendanceRepository.findBySchoolClassIdAndAcademicSessionIdAndTermId(classId, session.getId(), term.getId());
        } else {
            records = attendanceRepository.findByAcademicSessionIdAndTermId(session.getId(), term.getId());
            if (teacherClassIds != null) {
                Set<Long> scope = new HashSet<>(teacherClassIds);
                records = records.stream().filter(a -> scope.contains(a.getSchoolClass().getId())).toList();
            }
            if (date != null) {
                records = records.stream().filter(a -> a.getDate().equals(date)).toList();
            }
        }
        return records.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> listForStudent(Long studentId, Long sessionId, Long termId, List<Long> teacherClassIds) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        if (teacherClassIds != null && !teacherClassIds.contains(student.getSchoolClass().getId())) {
            throw new UnauthorizedScopeException("You can only view attendance for students in classes you teach.");
        }
        AcademicSession session = periodResolver.resolveSession(sessionId);
        Term term = periodResolver.resolveTerm(termId);
        return attendanceRepository.findByStudentIdAndAcademicSessionIdAndTermId(studentId, session.getId(), term.getId())
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public AttendanceSubmitSummaryResponse submitBulk(AttendanceBulkSubmitRequest request, List<Long> teacherClassIds) {
        if (teacherClassIds != null && !teacherClassIds.contains(request.classId())) {
            throw new UnauthorizedScopeException("You can only record attendance for classes you teach.");
        }
        SchoolClass schoolClass = schoolClassRepository.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.classId()));
        AcademicSession session = periodResolver.resolveSession(request.academicSessionId());
        Term term = periodResolver.resolveTerm(request.termId());

        int present = 0, absent = 0, late = 0;
        // Mirrors the frontend mock's behavior exactly: each submission
        // appends new rows rather than upserting by date — resubmitting
        // the same class+date twice will accumulate duplicate records.
        // Flagged here rather than silently "fixed", since deduping would
        // be a behavior change beyond what was asked.
        for (AttendanceEntryDto entry : request.entries()) {
            Student student = studentRepository.findById(entry.studentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + entry.studentId()));
            Attendance attendance = Attendance.builder()
                    .student(student)
                    .schoolClass(schoolClass)
                    .academicSession(session)
                    .term(term)
                    .date(request.date())
                    .status(entry.status())
                    .build();
            attendanceRepository.save(attendance);
            switch (entry.status()) {
                case Present -> present++;
                case Absent -> absent++;
                case Late -> late++;
            }
        }

        int total = request.entries().size();
        double rate = total == 0 ? 0.0 : round1(((present + late * 0.5) / total) * 100.0);
        return new AttendanceSubmitSummaryResponse(present, absent, late, total, rate);
    }

    @Override
    @Transactional
    public AttendanceRecordResponse updateAttendance(Long attendanceId, AttendanceUpdateRequest request, List<Long> teacherClassIds) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + attendanceId));
        if (teacherClassIds != null && !teacherClassIds.contains(attendance.getSchoolClass().getId())) {
            throw new UnauthorizedScopeException("You can only update attendance for classes you teach.");
        }
        attendance.setStatus(request.status());
        attendance.setRemarks(request.remarks());
        attendance = attendanceRepository.save(attendance);
        return toResponse(attendance);
    }

    @Override
    @Transactional
    public void deleteAttendance(Long attendanceId, List<Long> teacherClassIds) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + attendanceId));
        if (teacherClassIds != null && !teacherClassIds.contains(attendance.getSchoolClass().getId())) {
            throw new UnauthorizedScopeException("You can only delete attendance for classes you teach.");
        }
        attendanceRepository.delete(attendance);
    }

    private AttendanceRecordResponse toResponse(Attendance a) {
        return new AttendanceRecordResponse(
                a.getId(), a.getStudent().getName(), a.getSchoolClass().getName(),
                a.getDate().toString(), a.getStatus().name(), a.getRemarks());
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
