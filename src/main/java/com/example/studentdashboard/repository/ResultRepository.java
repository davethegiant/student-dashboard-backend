package com.example.studentdashboard.repository;

import com.example.studentdashboard.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findByStudentIdAndAcademicSessionIdAndTermId(Long studentId, Long academicSessionId, Long termId);

    /** Powers the student profile's trend chart — all of a student's results across a whole session, grouped by term in the service layer. */
    List<Result> findByStudentIdAndAcademicSessionId(Long studentId, Long academicSessionId);

    List<Result> findByAcademicSessionIdAndTermId(Long academicSessionId, Long termId);

    List<Result> findBySchoolClassIdAndAcademicSessionIdAndTermId(Long classId, Long academicSessionId, Long termId);

    List<Result> findBySubjectIdAndAcademicSessionIdAndTermId(Long subjectId, Long academicSessionId, Long termId);

    List<Result> findByStudentIdIn(List<Long> studentIds);

    List<Result> findByStudentIdInAndAcademicSessionIdAndTermId(List<Long> studentIds, Long academicSessionId, Long termId);

    List<Result> findByStudentIdInAndAcademicSessionId(List<Long> studentIds, Long academicSessionId);

    Optional<Result> findByStudentIdAndSubjectIdAndAcademicSessionIdAndTermId(
            Long studentId, Long subjectId, Long academicSessionId, Long termId);
}
