package com.example.studentdashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Named "Result" per the spec, though the frontend calls this concept
 * "performance". One row per student, per subject, per session + term —
 * the unique constraint means re-submitting the same combination is an
 * update, not a duplicate, matching the frontend's upsert behavior.
 * CA + Exam are broken out separately (not a single "score") because the
 * real entry form and student profile both require that breakdown.
 */
@Entity
@Table(name = "result", uniqueConstraints = {
        @UniqueConstraint(name = "uk_result_unique_entry",
                columnNames = {"student_id", "subject_id", "academic_session_id", "term_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Result extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_session_id", nullable = false)
    private AcademicSession academicSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    /** Continuous Assessment score, out of 40. */
    @Column(nullable = false)
    private Integer ca;

    /** Examination score, out of 60. */
    @Column(nullable = false)
    private Integer exam;

    @Column(nullable = false)
    private Integer total;

    @Column(nullable = false, length = 2)
    private String grade;

    @Column(length = 500)
    private String comment;

    /** A, B, C, D, E, F on the standard 100-point scale used across the app. */
    public static String gradeFor(int total) {
        if (total >= 75) return "A";
        if (total >= 65) return "B";
        if (total >= 55) return "C";
        if (total >= 45) return "D";
        if (total >= 40) return "E";
        return "F";
    }
}
