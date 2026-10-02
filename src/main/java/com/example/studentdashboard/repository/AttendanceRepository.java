package com.example.studentdashboard.repository;

import com.example.studentdashboard.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByStudentIdAndAcademicSessionIdAndTermId(Long studentId, Long academicSessionId, Long termId);

    List<Attendance> findByAcademicSessionIdAndTermId(Long academicSessionId, Long termId);

    List<Attendance> findBySchoolClassIdAndAcademicSessionIdAndTermId(Long classId, Long academicSessionId, Long termId);

    List<Attendance> findBySchoolClassIdAndDate(Long classId, LocalDate date);

    List<Attendance> findByStudentIdIn(List<Long> studentIds);

    List<Attendance> findByStudentIdInAndAcademicSessionIdAndTermId(List<Long> studentIds, Long academicSessionId, Long termId);

    List<Attendance> findByStudentIdInAndAcademicSessionId(List<Long> studentIds, Long academicSessionId);
}
