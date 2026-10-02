package com.example.studentdashboard.repository;

import com.example.studentdashboard.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    Optional<SchoolClass> findByClassCode(String classCode);

    List<SchoolClass> findByIdIn(List<Long> ids);
}
