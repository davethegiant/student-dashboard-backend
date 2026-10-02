package com.example.studentdashboard.repository;

import com.example.studentdashboard.entity.AcademicSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcademicSessionRepository extends JpaRepository<AcademicSession, Long> {

    Optional<AcademicSession> findByLabel(String label);

    Optional<AcademicSession> findByCurrentTrue();
}
