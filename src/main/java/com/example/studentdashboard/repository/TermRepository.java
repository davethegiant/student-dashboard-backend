package com.example.studentdashboard.repository;

import com.example.studentdashboard.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {

    Optional<Term> findByName(String name);

    List<Term> findAllByOrderBySortOrderAsc();

    Optional<Term> findByCurrentTrue();
}
