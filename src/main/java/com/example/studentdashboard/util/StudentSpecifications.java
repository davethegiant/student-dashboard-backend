package com.example.studentdashboard.util;

import com.example.studentdashboard.entity.Gender;
import com.example.studentdashboard.entity.RecordStatus;
import com.example.studentdashboard.entity.Student;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Composable JPA Specifications for the student list endpoint's real
 * database-level search/filter/pagination (GET /api/students). Risk-level
 * filtering deliberately lives outside this class — see StudentService —
 * because risk is computed from Result + Attendance, not a stored column.
 */
public final class StudentSpecifications {

    private StudentSpecifications() {
    }

    public static Specification<Student> withFilters(String search, Long classId, Gender gender, RecordStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("studentCode")), like)));
            }
            if (classId != null) {
                predicates.add(cb.equal(root.get("schoolClass").get("id"), classId));
            }
            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
