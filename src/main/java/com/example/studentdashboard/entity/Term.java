package com.example.studentdashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** e.g. "First Term". sortOrder drives display/trend-chart ordering (1, 2, 3). */
@Entity
@Table(name = "term")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    /** Exactly one row should have this true — see DataSeeder / admin tooling. */
    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private boolean current = false;
}
