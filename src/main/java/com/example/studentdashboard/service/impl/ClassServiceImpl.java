package com.example.studentdashboard.service.impl;

import com.example.studentdashboard.dto.request.ClassRequest;
import com.example.studentdashboard.dto.response.ClassDetailResponse;
import com.example.studentdashboard.dto.response.ClassOverviewResponse;
import com.example.studentdashboard.dto.response.ClassSummaryResponse;
import com.example.studentdashboard.dto.response.RiskInfo;
import com.example.studentdashboard.dto.response.RosterStudentResponse;
import com.example.studentdashboard.dto.response.StudentSummaryResponse;
import com.example.studentdashboard.dto.response.TopPerformerResponse;
import com.example.studentdashboard.entity.AcademicSession;
import com.example.studentdashboard.entity.Attendance;
import com.example.studentdashboard.entity.RecordStatus;
import com.example.studentdashboard.entity.Result;
import com.example.studentdashboard.entity.SchoolClass;
import com.example.studentdashboard.entity.Student;
import com.example.studentdashboard.entity.Teacher;
import com.example.studentdashboard.entity.Term;
import com.example.studentdashboard.exception.ConflictException;
import com.example.studentdashboard.exception.ResourceNotFoundException;
import com.example.studentdashboard.exception.UnauthorizedScopeException;
import com.example.studentdashboard.repository.AttendanceRepository;
import com.example.studentdashboard.repository.ResultRepository;
import com.example.studentdashboard.repository.SchoolClassRepository;
import com.example.studentdashboard.repository.StudentRepository;
import com.example.studentdashboard.repository.TeacherRepository;
import com.example.studentdashboard.service.AcademicMetricsService;
import com.example.studentdashboard.service.ClassService;
import com.example.studentdashboard.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final ResultRepository resultRepository;
    private final AttendanceRepository attendanceRepository;
    private final TeacherRepository teacherRepository;
    private final AcademicMetricsService metricsService;
    private final RiskService riskService;
    private final AcademicPeriodResolver periodResolver;

    @Override
    @Transactional(readOnly = true)
    public List<ClassOverviewResponse> listClasses(Long sessionId, Long termId, List<Long> teacherClassIds) {
        AcademicSession session = periodResolver.resolveSession(sessionId);
        Term term = periodResolver.resolveTerm(termId);

        List<SchoolClass> classes = schoolClassRepository.findAll();
        if (teacherClassIds != null) {
            Set<Long> scope = new HashSet<>(teacherClassIds);
            classes = classes.stream().filter(c -> scope.contains(c.getId())).toList();
        }

        return classes.stream().map(c -> toOverview(c, session, term)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClassDetailResponse getClassDetail(Long classId, Long sessionId, Long termId, List<Long> teacherClassIds) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));

        if (teacherClassIds != null && !teacherClassIds.contains(classId)) {
            throw new UnauthorizedScopeException("You can only view classes you teach.");
        }

        AcademicSession session = periodResolver.resolveSession(sessionId);
        Term term = periodResolver.resolveTerm(termId);

        List<Student> students = studentRepository.findBySchoolClassId(classId);
        List<StudentSummaryResponse> summaries = toStudentSummaries(students, session, term);

        TopPerformerResponse top = summaries.stream()
                .max(Comparator.comparingDouble(StudentSummaryResponse::avgScore))
                .map(s -> new TopPerformerResponse(s.id(), s.studentCode(), s.name(), s.avgScore()))
                .orElse(null);

        long atRisk = summaries.stream().filter(s -> !s.risk().level().equals("LOW")).count();
        double classAvg = average(summaries, StudentSummaryResponse::avgScore);
        double classAtt = average(summaries, StudentSummaryResponse::attendanceRate);

        return new ClassDetailResponse(
                schoolClass.getId(), schoolClass.getClassCode(), schoolClass.getName(), schoolClass.getLevel(),
                formTeacherName(schoolClass), students.size(), round1(classAvg), round1(classAtt),
                top, (int) atRisk, summaries);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RosterStudentResponse> getRoster(Long classId) {
        if (!schoolClassRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found: " + classId);
        }
        return studentRepository.findBySchoolClassId(classId).stream()
                .filter(s -> s.getStatus() == RecordStatus.Active)
                .map(s -> new RosterStudentResponse(s.getId(), s.getStudentCode(), s.getName()))
                .toList();
    }

    @Override
    @Transactional
    public ClassSummaryResponse createClass(ClassRequest request) {
        if (schoolClassRepository.findByClassCode(request.classCode()).isPresent()) {
            throw new ConflictException("Class code \"" + request.classCode() + "\" is already in use.");
        }
        SchoolClass schoolClass = SchoolClass.builder()
                .classCode(request.classCode())
                .name(request.name())
                .level(request.level())
                .formTeacher(resolveFormTeacher(request.formTeacherId()))
                .build();
        schoolClass = schoolClassRepository.save(schoolClass);
        return toSummary(schoolClass);
    }

    @Override
    @Transactional
    public ClassSummaryResponse updateClass(Long classId, ClassRequest request) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));

        boolean codeChanged = !schoolClass.getClassCode().equalsIgnoreCase(request.classCode());
        if (codeChanged && schoolClassRepository.findByClassCode(request.classCode()).isPresent()) {
            throw new ConflictException("Class code \"" + request.classCode() + "\" is already in use.");
        }

        schoolClass.setClassCode(request.classCode());
        schoolClass.setName(request.name());
        schoolClass.setLevel(request.level());
        schoolClass.setFormTeacher(resolveFormTeacher(request.formTeacherId()));
        schoolClass = schoolClassRepository.save(schoolClass);
        return toSummary(schoolClass);
    }

    @Override
    @Transactional
    public void deleteClass(Long classId) {
        if (!schoolClassRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found: " + classId);
        }
        if (!studentRepository.findBySchoolClassId(classId).isEmpty()) {
            throw new ConflictException("Cannot delete a class that still has students assigned to it.");
        }
        schoolClassRepository.deleteById(classId);
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private ClassOverviewResponse toOverview(SchoolClass c, AcademicSession session, Term term) {
        List<Student> students = studentRepository.findBySchoolClassId(c.getId());
        List<StudentSummaryResponse> summaries = toStudentSummaries(students, session, term);

        double avg = average(summaries, StudentSummaryResponse::avgScore);
        double att = average(summaries, StudentSummaryResponse::attendanceRate);
        long atRisk = summaries.stream().filter(s -> !s.risk().level().equals("LOW")).count();

        return new ClassOverviewResponse(
                c.getId(), c.getClassCode(), c.getName(), c.getLevel(),
                formTeacherName(c), students.size(), round1(avg), round1(att), (int) atRisk);
    }

    private List<StudentSummaryResponse> toStudentSummaries(List<Student> students, AcademicSession session, Term term) {
        if (students.isEmpty()) return List.of();
        List<Long> ids = students.stream().map(Student::getId).toList();
        List<Result> results = resultRepository.findByStudentIdInAndAcademicSessionIdAndTermId(ids, session.getId(), term.getId());
        List<Attendance> attendance = attendanceRepository.findByStudentIdInAndAcademicSessionIdAndTermId(ids, session.getId(), term.getId());
        Map<Long, Double> avgMap = metricsService.averageScoreByStudent(results);
        Map<Long, Double> attMap = metricsService.attendanceRateByStudent(attendance);

        return students.stream().map(s -> {
            double avg = avgMap.getOrDefault(s.getId(), 0.0);
            double att = attMap.getOrDefault(s.getId(), 0.0);
            RiskInfo risk = riskService.computeRisk(avg, att);
            return new StudentSummaryResponse(
                    s.getId(), s.getStudentCode(), s.getName(), s.getGender().name(),
                    s.getSchoolClass().getId(), s.getSchoolClass().getName(), s.getStatus().name(),
                    avg, att, risk);
        }).toList();
    }

    private String formTeacherName(SchoolClass c) {
        return c.getFormTeacher() == null ? "Unassigned" : c.getFormTeacher().getName();
    }

    private Teacher resolveFormTeacher(Long teacherId) {
        if (teacherId == null) return null;
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + teacherId));
    }

    private ClassSummaryResponse toSummary(SchoolClass c) {
        return new ClassSummaryResponse(c.getId(), c.getClassCode(), c.getName(), c.getLevel());
    }

    private double average(List<StudentSummaryResponse> list, java.util.function.ToDoubleFunction<StudentSummaryResponse> fn) {
        return list.isEmpty() ? 0.0 : list.stream().mapToDouble(fn).average().orElse(0.0);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
