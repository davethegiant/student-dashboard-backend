package com.example.studentdashboard.service.impl;

import com.example.studentdashboard.dto.request.CommentCreateRequest;
import com.example.studentdashboard.dto.request.StudentRequest;
import com.example.studentdashboard.dto.response.AttendanceBreakdownResponse;
import com.example.studentdashboard.dto.response.ChartPointResponse;
import com.example.studentdashboard.dto.response.CommentResponse;
import com.example.studentdashboard.dto.response.PerformanceEntryResponse;
import com.example.studentdashboard.dto.response.RiskInfo;
import com.example.studentdashboard.dto.response.StudentProfileResponse;
import com.example.studentdashboard.dto.response.StudentSummaryResponse;
import com.example.studentdashboard.entity.AcademicSession;
import com.example.studentdashboard.entity.Attendance;
import com.example.studentdashboard.entity.Gender;
import com.example.studentdashboard.entity.RecordStatus;
import com.example.studentdashboard.entity.Result;
import com.example.studentdashboard.entity.SchoolClass;
import com.example.studentdashboard.entity.Student;
import com.example.studentdashboard.entity.Teacher;
import com.example.studentdashboard.entity.TeacherComment;
import com.example.studentdashboard.entity.Term;
import com.example.studentdashboard.exception.ResourceNotFoundException;
import com.example.studentdashboard.exception.UnauthorizedScopeException;
import com.example.studentdashboard.repository.AttendanceRepository;
import com.example.studentdashboard.repository.ResultRepository;
import com.example.studentdashboard.repository.SchoolClassRepository;
import com.example.studentdashboard.repository.StudentRepository;
import com.example.studentdashboard.repository.SubjectRepository;
import com.example.studentdashboard.repository.TeacherCommentRepository;
import com.example.studentdashboard.repository.TeacherRepository;
import com.example.studentdashboard.repository.TermRepository;
import com.example.studentdashboard.service.AcademicMetricsService;
import com.example.studentdashboard.service.RiskService;
import com.example.studentdashboard.service.StudentService;
import com.example.studentdashboard.util.CodeGenerator;
import com.example.studentdashboard.util.StudentSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ResultRepository resultRepository;
    private final AttendanceRepository attendanceRepository;
    private final TeacherCommentRepository teacherCommentRepository;
    private final SubjectRepository subjectRepository;
    private final TermRepository termRepository;
    private final TeacherRepository teacherRepository;
    private final AcademicMetricsService metricsService;
    private final RiskService riskService;
    private final AcademicPeriodResolver periodResolver;

    @Override
    @Transactional(readOnly = true)
    public Page<StudentSummaryResponse> listStudents(String search, Long classId, Gender gender, RecordStatus status,
                                                       String riskLevel, Long sessionId, Long termId,
                                                       List<Long> teacherClassIds, Pageable pageable) {
        AcademicSession session = periodResolver.resolveSession(sessionId);
        Term term = periodResolver.resolveTerm(termId);

        Specification<Student> spec = StudentSpecifications.withFilters(search, classId, gender, status);
        if (teacherClassIds != null) {
            // Empty list (teacher assigned to nothing) must yield zero results, not "no filter".
            List<Long> scope = teacherClassIds.isEmpty() ? List.of(-1L) : teacherClassIds;
            spec = spec.and((root, query, cb) -> root.get("schoolClass").get("id").in(scope));
        }

        boolean riskFilterApplied = riskLevel != null && !riskLevel.isBlank();

        if (!riskFilterApplied) {
            // Real DB-level pagination — the common, fast path.
            Page<Student> page = studentRepository.findAll(spec, pageable);
            List<StudentSummaryResponse> content = toSummaries(page.getContent(), session, term);
            return new PageImpl<>(content, pageable, page.getTotalElements());
        }

        // Risk is computed, not stored, so it can't be a SQL predicate — pull
        // every filtered student, compute+filter in memory, then paginate by hand.
        List<Student> all = studentRepository.findAll(spec);
        List<StudentSummaryResponse> filtered = toSummaries(all, session, term).stream()
                .filter(s -> s.risk().level().equalsIgnoreCase(riskLevel))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<StudentSummaryResponse> pageContent = start >= filtered.size() ? List.of() : filtered.subList(start, end);
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    @Override
    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile(Long studentId, Long sessionId, Long termId, List<Long> teacherClassIds) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        if (teacherClassIds != null && !teacherClassIds.contains(student.getSchoolClass().getId())) {
            throw new UnauthorizedScopeException("You can only view students in classes you teach.");
        }

        AcademicSession session = periodResolver.resolveSession(sessionId);
        Term term = periodResolver.resolveTerm(termId);

        List<Result> results = resultRepository.findByStudentIdAndAcademicSessionIdAndTermId(
                studentId, session.getId(), term.getId());
        List<Attendance> attendanceRecords = attendanceRepository.findByStudentIdAndAcademicSessionIdAndTermId(
                studentId, session.getId(), term.getId());

        double avg = metricsService.averageScore(results);
        double att = metricsService.attendanceRate(attendanceRecords);
        RiskInfo risk = riskService.computeRisk(avg, att);

        List<PerformanceEntryResponse> performance = results.stream()
                .map(r -> new PerformanceEntryResponse(
                        r.getId(), r.getSubject().getName(), r.getCa(), r.getExam(), r.getTotal(), r.getGrade(), r.getComment()))
                .toList();

        List<Result> sessionResults = resultRepository.findByStudentIdAndAcademicSessionId(studentId, session.getId());
        Map<Long, List<Result>> byTermId = sessionResults.stream()
                .collect(Collectors.groupingBy(r -> r.getTerm().getId()));
        List<ChartPointResponse> trend = termRepository.findAllByOrderBySortOrderAsc().stream()
                .map(t -> new ChartPointResponse(t.getName(), metricsService.averageScore(byTermId.getOrDefault(t.getId(), List.of()))))
                .toList();

        AttendanceBreakdownResponse breakdown = metricsService.attendanceBreakdown(attendanceRecords);

        DateTimeFormatter dateFmt = DateTimeFormatter.ISO_LOCAL_DATE;
        List<CommentResponse> comments = teacherCommentRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(c -> new CommentResponse(
                        c.getId(), c.getTeacher().getName(), c.getTerm().getName(), c.getComment(),
                        c.getCreatedAt().atZone(ZoneOffset.UTC).format(dateFmt)))
                .toList();

        return new StudentProfileResponse(
                student.getId(), student.getStudentCode(), student.getName(), student.getGender().name(),
                student.getSchoolClass().getId(), student.getSchoolClass().getName(), student.getStatus().name(),
                avg, att, (int) subjectRepository.count(),
                performance, trend, breakdown, risk, comments);
    }

    @Override
    @Transactional
    public StudentSummaryResponse createStudent(StudentRequest request) {
        SchoolClass schoolClass = schoolClassRepository.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.classId()));

        Student student = Student.builder()
                .studentCode(CodeGenerator.tempCode())
                .name(request.name())
                .gender(request.gender())
                .schoolClass(schoolClass)
                .dob(request.dob())
                .guardianName(request.guardianName())
                .guardianPhone(request.guardianPhone())
                .address(request.address())
                .status(RecordStatus.Active)
                .admittedDate(request.admittedDate())
                .build();
        student = studentRepository.save(student);
        student.setStudentCode(CodeGenerator.studentCode(student.getId()));
        student = studentRepository.save(student);

        return toSummary(student, 0.0, 0.0);
    }

    @Override
    @Transactional
    public StudentSummaryResponse updateStudent(Long studentId, StudentRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        SchoolClass schoolClass = schoolClassRepository.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.classId()));

        student.setName(request.name());
        student.setGender(request.gender());
        student.setSchoolClass(schoolClass);
        student.setDob(request.dob());
        student.setGuardianName(request.guardianName());
        student.setGuardianPhone(request.guardianPhone());
        student.setAddress(request.address());
        student.setAdmittedDate(request.admittedDate());
        student = studentRepository.save(student);

        AcademicSession session = periodResolver.resolveSession(null);
        Term term = periodResolver.resolveTerm(null);
        List<Result> results = resultRepository.findByStudentIdAndAcademicSessionIdAndTermId(
                studentId, session.getId(), term.getId());
        List<Attendance> attendance = attendanceRepository.findByStudentIdAndAcademicSessionIdAndTermId(
                studentId, session.getId(), term.getId());
        return toSummary(student, metricsService.averageScore(results), metricsService.attendanceRate(attendance));
    }

    @Override
    @Transactional
    public void updateStatus(Long studentId, RecordStatus status) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        student.setStatus(status);
        studentRepository.save(student);
    }

    @Override
    @Transactional
    public void deleteStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }
        studentRepository.deleteById(studentId);
    }

    @Override
    @Transactional
    public CommentResponse addComment(Long studentId, CommentCreateRequest request, Long teacherId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + teacherId));
        AcademicSession session = periodResolver.resolveSession(request.academicSessionId());
        Term term = periodResolver.resolveTerm(request.termId());

        TeacherComment comment = TeacherComment.builder()
                .student(student)
                .teacher(teacher)
                .academicSession(session)
                .term(term)
                .comment(request.comment())
                .build();
        comment = teacherCommentRepository.save(comment);

        return new CommentResponse(
                comment.getId(), teacher.getName(), term.getName(), comment.getComment(),
                comment.getCreatedAt().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private List<StudentSummaryResponse> toSummaries(List<Student> students, AcademicSession session, Term term) {
        if (students.isEmpty()) return List.of();
        List<Long> ids = students.stream().map(Student::getId).toList();
        List<Result> results = resultRepository.findByStudentIdInAndAcademicSessionIdAndTermId(ids, session.getId(), term.getId());
        List<Attendance> attendance = attendanceRepository.findByStudentIdInAndAcademicSessionIdAndTermId(ids, session.getId(), term.getId());
        Map<Long, Double> avgMap = metricsService.averageScoreByStudent(results);
        Map<Long, Double> attMap = metricsService.attendanceRateByStudent(attendance);
        return students.stream()
                .map(s -> toSummary(s, avgMap.getOrDefault(s.getId(), 0.0), attMap.getOrDefault(s.getId(), 0.0)))
                .toList();
    }

    private StudentSummaryResponse toSummary(Student s, double avg, double att) {
        RiskInfo risk = riskService.computeRisk(avg, att);
        return new StudentSummaryResponse(
                s.getId(), s.getStudentCode(), s.getName(), s.getGender().name(),
                s.getSchoolClass().getId(), s.getSchoolClass().getName(), s.getStatus().name(),
                avg, att, risk);
    }
}
