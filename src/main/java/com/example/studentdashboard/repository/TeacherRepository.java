package com.example.studentdashboard.repository;

import com.example.studentdashboard.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByTeacherCode(String teacherCode);

    Optional<Teacher> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Optional<Teacher> findByUserId(Long userId);

    @Query("select t from Teacher t join t.classes c where c.id = :classId")
    List<Teacher> findByClassId(@Param("classId") Long classId);

    @Query("select count(t) from Teacher t join t.subjects s where s.id = :subjectId")
    long countBySubjectId(@Param("subjectId") Long subjectId);
}
