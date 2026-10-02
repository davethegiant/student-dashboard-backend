package com.example.studentdashboard.security;

import com.example.studentdashboard.entity.RecordStatus;
import com.example.studentdashboard.entity.Teacher;
import com.example.studentdashboard.entity.User;
import com.example.studentdashboard.repository.TeacherRepository;
import com.example.studentdashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("No account found for username: " + username));

        Long teacherId = null;
        boolean enabled = true;

        if (user.getRole() == Role.TEACHER) {
            Teacher teacher = teacherRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Teacher account is misconfigured — no linked teacher record for username: " + username));
            teacherId = teacher.getId();
            enabled = teacher.getStatus() == RecordStatus.Active;
        }

        return new UserPrincipal(user, teacherId, enabled);
    }
}
