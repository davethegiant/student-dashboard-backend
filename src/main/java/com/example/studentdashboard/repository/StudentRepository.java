package com.example.studentdashboard.repository;

import com.example.studentdashboard.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * Extends JpaSpecificationExecutor so StudentService can compose dynamic
 * search/class/gender/status predicates (see util.StudentSpecifications)
 * with real database-level pagination and sorting — risk-level filtering
 * is the one exception, handled in the service layer, since risk is a
 * computed value derived from Result + Attendance, not a stored column.
 */
public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    Optional<Student> findByStudentCode(String studentCode);

    List<Student> findBySchoolClassId(Long classId);

    List<Student> findBySchoolClassIdIn(List<Long> classIds);
}
