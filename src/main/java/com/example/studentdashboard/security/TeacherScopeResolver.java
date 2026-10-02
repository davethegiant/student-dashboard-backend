package com.example.studentdashboard.security;

import com.example.studentdashboard.entity.SchoolClass;
import com.example.studentdashboard.entity.Teacher;
import com.example.studentdashboard.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Every controller that touches class-scoped data (students, results,
 * attendance, dashboard, analytics, reports) calls this once to get the
 * caller's class scope — null for ADMIN (unrestricted), or the teacher's
 * assigned class IDs otherwise. The resulting list is what services use
 * to enforce scoping server-side.
 */
@Component
@RequiredArgsConstructor
public class TeacherScopeResolver {

    private final TeacherRepository teacherRepository;

    public List<Long> resolveClassScope(UserPrincipal principal) {
        if (principal.getRole() != Role.TEACHER) {
            return null;
        }
        Teacher teacher = teacherRepository.findById(principal.getTeacherId())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated as a teacher but no matching teacher record exists — id " + principal.getTeacherId()));
        return teacher.getClasses().stream().map(SchoolClass::getId).toList();
    }
}
