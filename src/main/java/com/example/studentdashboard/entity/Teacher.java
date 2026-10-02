package com.example.studentdashboard.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * A teacher *record* (name, contact info, class/subject assignments) is
 * separate from a teacher's *login access*. {@link #user} is null until an
 * admin explicitly grants login — a teacher can exist on file with no
 * account at all, which is a deliberate frontend feature, not a gap.
 */
@Entity
@Table(name = "teacher")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Teacher extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_code", nullable = false, unique = true, length = 20)
    private String teacherCode;

    @Column(nullable = false, length = 120)
    private String name;

    /** Contact email — independent of the login email on {@link User}, which may differ. */
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RecordStatus status = RecordStatus.Active;

    @Column(name = "joined_date")
    private LocalDate joinedDate;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinTable(
            name = "teacher_class",
            joinColumns = @JoinColumn(name = "teacher_id"),
            inverseJoinColumns = @JoinColumn(name = "class_id"))
    @Builder.Default
    private Set<SchoolClass> classes = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinTable(
            name = "teacher_subject",
            joinColumns = @JoinColumn(name = "teacher_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id"))
    @Builder.Default
    private Set<Subject> subjects = new HashSet<>();

    public boolean hasLogin() {
        return user != null;
    }
}
