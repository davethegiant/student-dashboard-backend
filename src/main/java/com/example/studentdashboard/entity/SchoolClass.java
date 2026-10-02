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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "school_class")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Business-facing code shown in the UI, e.g. "JSS1A". Distinct from the surrogate PK. */
    @Column(name = "class_code", nullable = false, unique = true, length = 20)
    private String classCode;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, length = 40)
    private String level;

    /**
     * Explicit, nullable — NOT derived from the teacher_class M2M table.
     * A class can have several teachers assigned across different subjects;
     * "form teacher" is a distinct, single, optional designation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_teacher_id")
    private Teacher formTeacher;
}
