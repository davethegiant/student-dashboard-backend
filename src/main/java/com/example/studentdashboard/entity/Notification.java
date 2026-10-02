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

import java.time.LocalDate;

/**
 * type is a free-form string on purpose (e.g. "attendance_risk",
 * "academic_risk", "improvement", "system", "report") — matches the
 * frontend's icon/label lookup keys exactly rather than a rigid enum.
 */
@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;
}
