package com.example.studentdashboard.repository;

import com.example.studentdashboard.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findBySubjectCode(String subjectCode);
}
