package com.example.studentdashboard.service.impl;

import com.example.studentdashboard.dto.request.ReportGenerateRequest;
import com.example.studentdashboard.dto.response.ReportResponse;
import com.example.studentdashboard.dto.response.RiskInfo;
import com.example.studentdashboard.entity.AcademicSession;
import com.example.studentdashboard.entity.Attendance;
import com.example.studentdashboard.entity.Result;
import com.example.studentdashboard.entity.SchoolClass;
import com.example.studentdashboard.entity.Student;
import com.example.studentdashboard.entity.Term;
import com.example.studentdashboard.exception.ResourceNotFoundException;
import com.example.studentdashboard.repository.AttendanceRepository;
import com.example.studentdashboard.repository.ResultRepository;
import com.example.studentdashboard.repository.SchoolClassRepository;
import com.example.studentdashboard.repository.StudentRepository;
import com.example.studentdashboard.repository.SubjectRepository;
import com.example.studentdashboard.repository.TeacherRepository;
import com.example.studentdashboard.service.AcademicMetricsService;
import com.example.studentdashboard.service.ReportService;
import com.example.studentdashboard.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final ResultRepository resultRepository;
    private final AttendanceRepository attendanceRepository;
    private final TeacherRepository teacherRepository;
    private final AcademicMetricsService metricsService;
    private final RiskService riskService;
    private final AcademicPeriodResolver periodResolver;

    private record ReportContent(String title, List<String> columns, List<List<String>> rows) {
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse generateReport(ReportGenerateRequest request, List<Long> teacherClassIds) {
        AcademicSession session = periodResolver.resolveSession(request.academicSessionId());
        Term term = periodResolver.resolveTerm(request.termId());

        ReportContent content = switch (request.type()) {
            case "student" -> buildStudentReport(request.studentId(), session, term, teacherClassIds);
            case "class" -> buildClassReport(request.classId(), session, term, teacherClassIds);
            case "attendance" -> buildAttendanceReport(request.classId(), session, term, teacherClassIds);
            case "at_risk" -> buildAtRiskReport(session, term, teacherClassIds);
            case "subject" -> buildSubjectReport(session, term, teacherClassIds);
            default -> throw new IllegalArgumentException("Unknown report type: " + request.type());
        };

        return new ReportResponse(content.title(), content.columns(), content.rows(),
                session.getLabel(), term.getName(), Instant.now().toString());
    }

    // -------------------------------------------------------------------
    // Report builders
    // -------------------------------------------------------------------

    private ReportContent buildStudentReport(Long studentId, AcademicSession session, Term term, List<Long> teacherClassIds) {
        if (studentId == null) throw new IllegalArgumentException("studentId is required for a student report.");
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        if (teacherClassIds != null && !teacherClassIds.contains(student.getSchoolClass().getId())) {
            return unauthorized("Student Performance Report");
        }

        List<Result> results = resultRepository.findByStudentIdAndAcademicSessionIdAndTermId(studentId, session.getId(), term.getId());
        List<String> columns = List.of("Subject", "CA", "Exam", "Total", "Grade");
        List<List<String>> rows = results.stream()
                .map(r -> List.of(r.getSubject().getName(), String.valueOf(r.getCa()), String.valueOf(r.getExam()),
                        String.valueOf(r.getTotal()), r.getGrade()))
                .toList();

        return new ReportContent("Student Performance Report — " + student.getName(), columns, rows);
    }

    private ReportContent buildClassReport(Long classId, AcademicSession session, Term term, List<Long> teacherClassIds) {
        if (classId == null) throw new IllegalArgumentException("classId is required for a class report.");
        if (teacherClassIds != null && !teacherClassIds.contains(classId)) {
            return unauthorized("Class Performance Report");
        }
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));

        List<Student> students = studentRepository.findBySchoolClassId(classId);
        List<Long> ids = students.stream().map(Student::getId).toList();
        List<Result> results = ids.isEmpty() ? List.of()
                : resultRepository.findByStudentIdInAndAcademicSessionIdAndTermId(ids, session.getId(), term.getId());
        List<Attendance> attendance = ids.isEmpty() ? List.of()
                : attendanceRepository.findByStudentIdInAndAcademicSessionIdAndTermId(ids, session.getId(), term.getId());
        Map<Long, Double> avgMap = metricsService.averageScoreByStudent(results);
        Map<Long, Double> attMap = metricsService.attendanceRateByStudent(attendance);

        List<String> columns = List.of("Student", "Average", "Attendance", "Risk");
        List<List<String>> rows = students.stream().map(s -> {
            double avg = avgMap.getOrDefault(s.getId(), 0.0);
            double att = attMap.getOrDefault(s.getId(), 0.0);
            String badge = riskService.computeRisk(avg, att).badge();
            return List.of(s.getName(), avg + "%", att + "%", badge);
        }).toList();

        return new ReportContent("Class Performance Report — " + schoolClass.getName(), columns, rows);
    }

    private ReportContent buildAttendanceReport(Long classId, AcademicSession session, Term term, List<Long> teacherClassIds) {
        if (classId != null && teacherClassIds != null && !teacherClassIds.contains(classId)) {
            return unauthorized("Attendance Report");
        }

        String classLabel;
        List<Student> pool;
        if (classId != null) {
            SchoolClass schoolClass = schoolClassRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));
            classLabel = schoolClass.getName();
            pool = studentRepository.findBySchoolClassId(classId);
        } else {
            classLabel = teacherClassIds != null ? "All My Classes" : "All Classes";
            pool = teacherClassIds != null ? studentRepository.findBySchoolClassIdIn(teacherClassIds) : studentRepository.findAll();
        }

        List<Long> ids = pool.stream().map(Student::getId).toList();
        List<Attendance> attendance = ids.isEmpty() ? List.of()
                : attendanceRepository.findByStudentIdInAndAcademicSessionIdAndTermId(ids, session.getId(), term.getId());
        Map<Long, List<Attendance>> byStudent = attendance.stream()
                .collect(Collectors.groupingBy(a -> a.getStudent().getId()));

        List<String> columns = List.of("Student", "Present", "Absent", "Late", "Rate");
        List<List<String>> rows = pool.stream().map(s -> {
            List<Attendance> records = byStudent.getOrDefault(s.getId(), List.of());
            var breakdown = metricsService.attendanceBreakdown(records);
            double rate = metricsService.attendanceRate(records);
            return List.of(s.getName(), String.valueOf(breakdown.present()), String.valueOf(breakdown.absent()),
                    String.valueOf(breakdown.late()), rate + "%");
        }).toList();

        return new ReportContent("Attendance Report — " + classLabel, columns, rows);
    }

    private ReportContent buildAtRiskReport(AcademicSession session, Term term, List<Long> teacherClassIds) {
        List<Student> pool = teacherClassIds != null
                ? studentRepository.findBySchoolClassIdIn(teacherClassIds)
                : studentRepository.findAll();
        List<Long> ids = pool.stream().map(Student::getId).toList();
        List<Result> results = ids.isEmpty() ? List.of()
                : resultRepository.findByStudentIdInAndAcademicSessionIdAndTermId(ids, session.getId(), term.getId());
        List<Attendance> attendance = ids.isEmpty() ? List.of()
                : attendanceRepository.findByStudentIdInAndAcademicSessionIdAndTermId(ids, session.getId(), term.getId());
        Map<Long, Double> avgMap = metricsService.averageScoreByStudent(results);
        Map<Long, Double> attMap = metricsService.attendanceRateByStudent(attendance);

        List<String> columns = List.of("Student", "Class", "Average", "Attendance", "Risk Level", "Reason");
        List<List<String>> rows = pool.stream()
                .map(s -> {
                    double avg = avgMap.getOrDefault(s.getId(), 0.0);
                    double att = attMap.getOrDefault(s.getId(), 0.0);
                    RiskInfo risk = riskService.computeRisk(avg, att);
                    if (risk.level().equals("LOW")) return null;
                    return List.of(s.getName(), s.getSchoolClass().getName(), avg + "%", att + "%",
                            risk.level(), String.join(" ", risk.reasons()));
                })
                .filter(Objects::nonNull)
                .toList();

        return new ReportContent("At-Risk Student Report", columns, rows);
    }

    private ReportContent buildSubjectReport(AcademicSession session, Term term, List<Long> teacherClassIds) {
        List<String> columns = List.of("Subject", "Average Score", "Classes Covered", "Teachers");
        List<List<String>> rows = subjectRepository.findAll().stream().map(s -> {
            List<Result> subjResults = resultRepository.findBySubjectIdAndAcademicSessionIdAndTermId(s.getId(), session.getId(), term.getId());
            List<Result> scoped;
            if (teacherClassIds != null) {
                Set<Long> scope = new HashSet<>(teacherClassIds);
                scoped = subjResults.stream().filter(r -> scope.contains(r.getSchoolClass().getId())).toList();
            } else {
                scoped = subjResults;
            }
            double avg = metricsService.averageScore(scoped);
            long classCount = scoped.stream().map(r -> r.getSchoolClass().getId()).distinct().count();
            long teacherCount = teacherRepository.countBySubjectId(s.getId());
            return List.of(s.getName(), String.valueOf(avg), String.valueOf(classCount), String.valueOf(teacherCount));
        }).toList();

        return new ReportContent("Subject Performance Report", columns, rows);
    }

    private ReportContent unauthorized(String title) {
        return new ReportContent(title, List.of("Notice"),
                List.of(List.of("You can only generate reports for classes you teach.")));
    }
}
