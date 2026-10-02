package com.example.studentdashboard.service.impl;

import com.example.studentdashboard.dto.response.ActivityResponse;
import com.example.studentdashboard.dto.response.AdminDashboardResponse;
import com.example.studentdashboard.dto.response.AttendanceOverviewResponse;
import com.example.studentdashboard.dto.response.ChartPointResponse;
import com.example.studentdashboard.dto.response.DashboardTotalsResponse;
import com.example.studentdashboard.dto.response.StudentSummaryResponse;
import com.example.studentdashboard.dto.response.TeacherDashboardResponse;
import com.example.studentdashboard.dto.response.TeacherDashboardTotalsResponse;
import com.example.studentdashboard.entity.AcademicSession;
import com.example.studentdashboard.entity.Attendance;
import com.example.studentdashboard.entity.AttendanceStatus;
import com.example.studentdashboard.entity.RecordStatus;
import com.example.studentdashboard.entity.Result;
import com.example.studentdashboard.entity.SchoolClass;
import com.example.studentdashboard.entity.Student;
import com.example.studentdashboard.entity.Teacher;
import com.example.studentdashboard.entity.Term;
import com.example.studentdashboard.exception.ResourceNotFoundException;
import com.example.studentdashboard.repository.ActivityLogEntryRepository;
import com.example.studentdashboard.repository.AttendanceRepository;
import com.example.studentdashboard.repository.ResultRepository;
import com.example.studentdashboard.repository.SchoolClassRepository;
import com.example.studentdashboard.repository.StudentRepository;
import com.example.studentdashboard.repository.TeacherRepository;
import com.example.studentdashboard.repository.TermRepository;
import com.example.studentdashboard.service.AcademicMetricsService;
import com.example.studentdashboard.service.DashboardService;
import com.example.studentdashboard.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ResultRepository resultRepository;
    private final AttendanceRepository attendanceRepository;
    private final TermRepository termRepository;
    private final ActivityLogEntryRepository activityLogEntryRepository;
    private final AcademicMetricsService metricsService;
    private final RiskService riskService;
    private final AcademicPeriodResolver periodResolver;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard(Long sessionId, Long termId, Long classId) {
        AcademicSession session = periodResolver.resolveSession(sessionId);
        Term term = periodResolver.resolveTerm(termId);

        // Scoped pool — drives totals.avg*/atRisk, trends, top performers, needs-attention.
        List<Student> pool = (classId != null ? studentRepository.findBySchoolClassId(classId) : studentRepository.findAll())
                .stream().filter(s -> s.getStatus() == RecordStatus.Active).toList();
        List<Long> poolIds = pool.stream().map(Student::getId).toList();

        List<Result> poolResults = poolIds.isEmpty() ? List.of()
                : resultRepository.findByStudentIdInAndAcademicSessionIdAndTermId(poolIds, session.getId(), term.getId());
        List<Attendance> poolAttendance = poolIds.isEmpty() ? List.of()
                : attendanceRepository.findByStudentIdInAndAcademicSessionIdAndTermId(poolIds, session.getId(), term.getId());
        Map<Long, Double> avgMap = metricsService.averageScoreByStudent(poolResults);
        Map<Long, Double> attMap = metricsService.attendanceRateByStudent(poolAttendance);

        double avgPerformance = average(pool, avgMap);
        double avgAttendance = average(pool, attMap);
        long atRisk = pool.stream()
                .filter(s -> !riskService.computeRisk(avgMap.getOrDefault(s.getId(), 0.0), attMap.getOrDefault(s.getId(), 0.0)).level().equals("LOW"))
                .count();

        // Unscoped — totals.totalStudents/totalTeachers/totalClasses always reflect the whole school.
        long totalStudents = studentRepository.findAll().stream().filter(s -> s.getStatus() == RecordStatus.Active).count();
        long totalTeachers = teacherRepository.findAll().stream().filter(t -> t.getStatus() == RecordStatus.Active).count();
        long totalClasses = schoolClassRepository.count();

        DashboardTotalsResponse totals = new DashboardTotalsResponse(
                totalStudents, totalTeachers, totalClasses, round1(avgPerformance), round1(avgAttendance), atRisk);

        // Performance-by-class chart always shows every class, regardless of the classId filter.
        List<Result> allResults = resultRepository.findByAcademicSessionIdAndTermId(session.getId(), term.getId());
        Map<Long, List<Result>> resultsByClass = allResults.stream()
                .collect(Collectors.groupingBy(r -> r.getSchoolClass().getId()));
        List<ChartPointResponse> performanceByClass = schoolClassRepository.findAll().stream()
                .map(c -> new ChartPointResponse(c.getName(), metricsService.averageScore(resultsByClass.getOrDefault(c.getId(), List.of()))))
                .toList();

        // Attendance overview IS scoped by the classId filter.
        List<Attendance> overviewAttendance = classId != null
                ? attendanceRepository.findBySchoolClassIdAndAcademicSessionIdAndTermId(classId, session.getId(), term.getId())
                : attendanceRepository.findByAcademicSessionIdAndTermId(session.getId(), term.getId());
        AttendanceOverviewResponse attendanceOverview = new AttendanceOverviewResponse(
                (int) overviewAttendance.stream().filter(a -> a.getStatus() == AttendanceStatus.Present).count(),
                (int) overviewAttendance.stream().filter(a -> a.getStatus() == AttendanceStatus.Absent).count(),
                (int) overviewAttendance.stream().filter(a -> a.getStatus() == AttendanceStatus.Late).count());

        List<Term> allTermsOrdered = termRepository.findAllByOrderBySortOrderAsc();

        List<Result> poolSessionResults = poolIds.isEmpty() ? List.of()
                : resultRepository.findByStudentIdInAndAcademicSessionId(poolIds, session.getId());
        Map<Long, List<Result>> poolResultsByTerm = poolSessionResults.stream()
                .collect(Collectors.groupingBy(r -> r.getTerm().getId()));
        List<ChartPointResponse> performanceTrend = allTermsOrdered.stream()
                .map(t -> new ChartPointResponse(t.getName(), metricsService.averageScore(poolResultsByTerm.getOrDefault(t.getId(), List.of()))))
                .toList();

        List<Attendance> poolSessionAttendance = poolIds.isEmpty() ? List.of()
                : attendanceRepository.findByStudentIdInAndAcademicSessionId(poolIds, session.getId());
        Map<Long, List<Attendance>> poolAttendanceByTerm = poolSessionAttendance.stream()
                .collect(Collectors.groupingBy(a -> a.getTerm().getId()));
        List<ChartPointResponse> attendanceTrend = allTermsOrdered.stream()
                .map(t -> new ChartPointResponse(t.getName(), metricsService.attendanceRate(poolAttendanceByTerm.getOrDefault(t.getId(), List.of()))))
                .toList();

        List<StudentSummaryResponse> summaries = toSummaries(pool, avgMap, attMap);
        List<StudentSummaryResponse> topPerformers = summaries.stream()
                .sorted(Comparator.comparingDouble(StudentSummaryResponse::avgScore).reversed())
                .limit(5)
                .toList();
        List<StudentSummaryResponse> needsAttention = summaries.stream()
                .filter(s -> !s.risk().level().equals("LOW"))
                .sorted(needsAttentionComparator())
                .limit(8)
                .toList();

        DateTimeFormatter dateFmt = DateTimeFormatter.ISO_LOCAL_DATE;
        List<ActivityResponse> recentActivity = activityLogEntryRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .limit(6)
                .map(a -> new ActivityResponse(a.getId(), a.getType(), a.getMessage(), a.getActor(),
                        a.getCreatedAt().atZone(ZoneOffset.UTC).format(dateFmt)))
                .toList();

        return new AdminDashboardResponse(totals, performanceByClass, attendanceOverview, performanceTrend,
                attendanceTrend, topPerformers, needsAttention, recentActivity);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherDashboardResponse getTeacherDashboard(Long teacherId, Long sessionId, Long termId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + teacherId));
        AcademicSession session = periodResolver.resolveSession(sessionId);
        Term term = periodResolver.resolveTerm(termId);

        List<SchoolClass> classes = List.copyOf(teacher.getClasses());
        List<Long> classIds = classes.stream().map(SchoolClass::getId).toList();

        List<Student> pool = classIds.isEmpty() ? List.of()
                : studentRepository.findBySchoolClassIdIn(classIds).stream()
                        .filter(s -> s.getStatus() == RecordStatus.Active).toList();
        List<Long> poolIds = pool.stream().map(Student::getId).toList();

        List<Result> poolResults = poolIds.isEmpty() ? List.of()
                : resultRepository.findByStudentIdInAndAcademicSessionIdAndTermId(poolIds, session.getId(), term.getId());
        List<Attendance> poolAttendance = poolIds.isEmpty() ? List.of()
                : attendanceRepository.findByStudentIdInAndAcademicSessionIdAndTermId(poolIds, session.getId(), term.getId());
        Map<Long, Double> avgMap = metricsService.averageScoreByStudent(poolResults);
        Map<Long, Double> attMap = metricsService.attendanceRateByStudent(poolAttendance);

        double avgPerformance = average(pool, avgMap);
        double avgAttendance = average(pool, attMap);
        long atRisk = pool.stream()
                .filter(s -> !riskService.computeRisk(avgMap.getOrDefault(s.getId(), 0.0), attMap.getOrDefault(s.getId(), 0.0)).level().equals("LOW"))
                .count();

        TeacherDashboardTotalsResponse totals = new TeacherDashboardTotalsResponse(
                classIds.size(), pool.size(), round1(avgPerformance), round1(avgAttendance), atRisk);

        Map<Long, List<Result>> resultsByClass = poolResults.stream()
                .collect(Collectors.groupingBy(r -> r.getSchoolClass().getId()));
        List<ChartPointResponse> classPerformance = classes.stream()
                .map(c -> new ChartPointResponse(c.getName(), metricsService.averageScore(resultsByClass.getOrDefault(c.getId(), List.of()))))
                .toList();

        List<Attendance> poolSessionAttendance = poolIds.isEmpty() ? List.of()
                : attendanceRepository.findByStudentIdInAndAcademicSessionId(poolIds, session.getId());
        Map<Long, List<Attendance>> attByTerm = poolSessionAttendance.stream()
                .collect(Collectors.groupingBy(a -> a.getTerm().getId()));
        List<ChartPointResponse> attendanceTrend = termRepository.findAllByOrderBySortOrderAsc().stream()
                .map(t -> new ChartPointResponse(t.getName(), metricsService.attendanceRate(attByTerm.getOrDefault(t.getId(), List.of()))))
                .toList();

        // Deliberately scoped to the teacher's own pool — the original mock
        // computed this school-wide, which would leak whole-school subject
        // data to a teacher. Kept consistent with the scoping enforced
        // everywhere else in this backend (reports, analytics, class access).
        Map<Long, List<Result>> resultsBySubject = poolResults.stream()
                .collect(Collectors.groupingBy(r -> r.getSubject().getId()));
        List<ChartPointResponse> subjectPerformance = teacher.getSubjects().stream()
                .map(s -> new ChartPointResponse(s.getName(), metricsService.averageScore(resultsBySubject.getOrDefault(s.getId(), List.of()))))
                .toList();

        List<StudentSummaryResponse> summaries = toSummaries(pool, avgMap, attMap);
        List<StudentSummaryResponse> needsAttention = summaries.stream()
                .filter(s -> !s.risk().level().equals("LOW"))
                .sorted(needsAttentionComparator())
                .limit(8)
                .toList();

        return new TeacherDashboardResponse(totals, classPerformance, attendanceTrend, subjectPerformance, needsAttention);
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private double average(List<Student> students, Map<Long, Double> valueMap) {
        if (students.isEmpty()) return 0.0;
        return students.stream().mapToDouble(s -> valueMap.getOrDefault(s.getId(), 0.0)).average().orElse(0.0);
    }

    private List<StudentSummaryResponse> toSummaries(List<Student> students, Map<Long, Double> avgMap, Map<Long, Double> attMap) {
        return students.stream().map(s -> {
            double avg = avgMap.getOrDefault(s.getId(), 0.0);
            double att = attMap.getOrDefault(s.getId(), 0.0);
            return new StudentSummaryResponse(
                    s.getId(), s.getStudentCode(), s.getName(), s.getGender().name(),
                    s.getSchoolClass().getId(), s.getSchoolClass().getName(), s.getStatus().name(),
                    avg, att, riskService.computeRisk(avg, att));
        }).toList();
    }

    /** HIGH risk sorts before MEDIUM; within the same level, lowest average first (worst first). */
    private Comparator<StudentSummaryResponse> needsAttentionComparator() {
        return (a, b) -> {
            if (a.risk().level().equals(b.risk().level())) {
                return Double.compare(a.avgScore(), b.avgScore());
            }
            return a.risk().level().equals("HIGH") ? -1 : 1;
        };
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
