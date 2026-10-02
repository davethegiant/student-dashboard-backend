package com.example.studentdashboard.service.impl;

import com.example.studentdashboard.dto.request.TeacherCreateRequest;
import com.example.studentdashboard.dto.request.TeacherLoginRequest;
import com.example.studentdashboard.dto.request.TeacherRequest;
import com.example.studentdashboard.dto.response.TeacherSummaryResponse;
import com.example.studentdashboard.entity.RecordStatus;
import com.example.studentdashboard.entity.SchoolClass;
import com.example.studentdashboard.entity.Subject;
import com.example.studentdashboard.entity.Teacher;
import com.example.studentdashboard.entity.User;
import com.example.studentdashboard.exception.ConflictException;
import com.example.studentdashboard.exception.ResourceNotFoundException;
import com.example.studentdashboard.repository.SchoolClassRepository;
import com.example.studentdashboard.repository.SubjectRepository;
import com.example.studentdashboard.repository.TeacherRepository;
import com.example.studentdashboard.repository.UserRepository;
import com.example.studentdashboard.security.Role;
import com.example.studentdashboard.service.TeacherService;
import com.example.studentdashboard.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<TeacherSummaryResponse> listTeachers(String search, RecordStatus status) {
        String q = search == null ? null : search.toLowerCase();
        return teacherRepository.findAll().stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> q == null || q.isBlank()
                        || t.getName().toLowerCase().contains(q)
                        || t.getEmail().toLowerCase().contains(q))
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public TeacherSummaryResponse createTeacher(TeacherCreateRequest request) {
        TeacherRequest profile = request.profile();

        if (teacherRepository.findByEmailIgnoreCase(profile.email()).isPresent()) {
            throw new ConflictException("Email \"" + profile.email() + "\" is already used by another teacher.");
        }
        if (request.grantLogin()) {
            validateLoginGrantFields(request.loginUsername(), request.loginPassword());
        }

        Teacher teacher = Teacher.builder()
                .teacherCode(CodeGenerator.tempCode())
                .name(profile.name())
                .email(profile.email())
                .phone(profile.phone())
                .status(RecordStatus.Active)
                .joinedDate(profile.joinedDate())
                .classes(new HashSet<>(resolveClasses(profile.classIds())))
                .subjects(new HashSet<>(resolveSubjects(profile.subjectIds())))
                .build();
        teacher = teacherRepository.save(teacher);
        teacher.setTeacherCode(CodeGenerator.teacherCode(teacher.getId()));
        teacher = teacherRepository.save(teacher);

        if (request.grantLogin()) {
            grantLoginInternal(teacher, request.loginUsername(), request.loginPassword());
        }

        return toSummary(teacher);
    }

    @Override
    @Transactional
    public TeacherSummaryResponse updateTeacher(Long teacherId, TeacherRequest request) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + teacherId));

        boolean emailChanged = !teacher.getEmail().equalsIgnoreCase(request.email());
        if (emailChanged && teacherRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), teacherId)) {
            throw new ConflictException("Email \"" + request.email() + "\" is already used by another teacher.");
        }

        teacher.setName(request.name());
        teacher.setPhone(request.phone());
        teacher.setJoinedDate(request.joinedDate());
        teacher.setClasses(new HashSet<>(resolveClasses(request.classIds())));
        teacher.setSubjects(new HashSet<>(resolveSubjects(request.subjectIds())));

        if (emailChanged) {
            // Keep the login email (on User, if one exists) in sync with the contact email.
            if (teacher.getUser() != null && userRepository.existsByEmailIgnoreCase(request.email())) {
                throw new ConflictException("Email \"" + request.email() + "\" is already used by another account.");
            }
            if (teacher.getUser() != null) {
                teacher.getUser().setEmail(request.email());
            }
            teacher.setEmail(request.email());
        }

        teacher = teacherRepository.save(teacher);
        return toSummary(teacher);
    }

    @Override
    @Transactional
    public void updateStatus(Long teacherId, RecordStatus status) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + teacherId));
        teacher.setStatus(status);
        teacherRepository.save(teacher);
    }

    @Override
    @Transactional
    public void deleteTeacher(Long teacherId) {
        if (!teacherRepository.existsById(teacherId)) {
            throw new ResourceNotFoundException("Teacher not found: " + teacherId);
        }
        teacherRepository.deleteById(teacherId);
    }

    @Override
    @Transactional
    public TeacherSummaryResponse grantOrResetLogin(Long teacherId, TeacherLoginRequest request) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + teacherId));
        grantLoginInternal(teacher, request.username(), request.password());
        return toSummary(teacher);
    }

    @Override
    @Transactional
    public void revokeLogin(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + teacherId));
        if (teacher.getUser() != null) {
            // orphanRemoval=true on Teacher.user deletes the User row once unlinked.
            teacher.setUser(null);
            teacherRepository.save(teacher);
        }
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    /**
     * TeacherCreateRequest can't enforce this with plain Bean Validation
     * since loginUsername/loginPassword are only required when grantLogin
     * is true — {@code @Size} alone lets a null password through silently,
     * which would otherwise surface as an opaque DB constraint failure.
     */
    private void validateLoginGrantFields(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required when granting login access.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required when granting login access.");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
    }

    private void grantLoginInternal(Teacher teacher, String username, String rawPassword) {
        // A username "taken" by the teacher's OWN existing account isn't a
        // conflict — that's just them resetting their own password.
        boolean usernameTakenByOther = userRepository.findByUsernameIgnoreCase(username)
                .map(existing -> teacher.getUser() == null || !existing.getId().equals(teacher.getUser().getId()))
                .orElse(false);
        if (usernameTakenByOther) {
            throw new ConflictException("Username \"" + username + "\" is already taken.");
        }

        if (teacher.getUser() != null) {
            User user = teacher.getUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setEmail(teacher.getEmail());
            user.setFullName(teacher.getName());
            userRepository.save(user);
        } else {
            if (userRepository.existsByEmailIgnoreCase(teacher.getEmail())) {
                throw new ConflictException(
                        "Email \"" + teacher.getEmail() + "\" is already used by another account's login.");
            }
            User user = User.builder()
                    .username(username)
                    .email(teacher.getEmail())
                    .password(passwordEncoder.encode(rawPassword))
                    .fullName(teacher.getName())
                    .role(Role.TEACHER)
                    .build();
            teacher.setUser(user);
            teacherRepository.save(teacher);
        }
    }

    private List<SchoolClass> resolveClasses(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<SchoolClass> found = schoolClassRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more class IDs were not found.");
        }
        return found;
    }

    private List<Subject> resolveSubjects(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Subject> found = subjectRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more subject IDs were not found.");
        }
        return found;
    }

    private TeacherSummaryResponse toSummary(Teacher t) {
        List<String> classNames = t.getClasses().stream().map(SchoolClass::getName).sorted().toList();
        List<String> subjectNames = t.getSubjects().stream().map(Subject::getName).sorted().toList();
        return new TeacherSummaryResponse(
                t.getId(), t.getTeacherCode(), t.getName(), t.getEmail(), t.getPhone(),
                t.getStatus().name(), t.getJoinedDate() == null ? null : t.getJoinedDate().toString(),
                classNames, subjectNames, t.hasLogin());
    }
}
