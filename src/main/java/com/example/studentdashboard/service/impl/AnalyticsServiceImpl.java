package com.example.studentdashboard.service.impl;

import com.example.studentdashboard.dto.response.AnalyticsResponse;
import com.example.studentdashboard.dto.response.ChartPointResponse;
import com.example.studentdashboard.dto.response.RiskCountsResponse;
import com.example.studentdashboard.dto.response.RiskInfo;
import com.example.studentdashboard.dto.response.StudentSummaryResponse;
import com.example.studentdashboard.entity.AcademicSession;
import com.example.studentdashboard.entity.Attendance;
import com.example.studentdashboard.entity.RecordStatus;
import com.example.studentdashboard.entity.Result;
import com.example.studentdashboard.entity.SchoolClass;
import com.example.studentdashboard.entity.Student;
import com.example.studentdashboard.entity.Term;
import com.example.studentdashboard.repository.AttendanceRepository;
import com.example.studentdashboard.repository.ResultRepository;
import com.example.studentdashboard.repository.SchoolClassRepository;
import com.example.studentdashboard.repository.StudentRepository;
import com.example.studentdashboard.repository.SubjectRepository;
import com.example.studentdashboard.repository.TermRepository;
import com.example.studentdashboard.service.AcademicMetricsService;
import com.example.studentdashboard.service.AnalyticsService;
import com.example.studentdashboard.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final ResultRepository resultRepository;
    private final AttendanceRepository attendanceRepository;
    private final TermRepository termRepository;
    private final AcademicMetricsService metricsService;
    private final RiskService riskService;
    private final AcademicPeriodResolver periodResolver;

    @Override
    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(Long sessionId, Long termId, List<Long> teacherClassIds) {
        AcademicSession session = periodResolver.resolveSession(sessionId);
        Term term = periodResolver.resolveTerm(termId);

        List<SchoolClass> classes = teacherClassIds != null
                ? schoolClassRepository.findAllById(teacherClassIds)
                : schoolClassRepository.findAll();
        List<Student> pool = activePool(teacherClassIds);
        List<Long> poolIds = pool.stream().map(Student::getId).toList();

        List<Result> poolTermResults = poolIds.isEmpty() ? List.of()
                : resultRepository.findByStudentIdInAndAcademicSessionIdAndTermId(poolIds, session.getId(), term.getId());

        Map<Long, List<Result>> resultsByClass = poolTermResults.stream()
                .collect(Collectors.groupingBy(r -> r.getSchoolClass().getId()));
        List<ChartPointResponse> classComparison = classes.stream()
                .map(c -> new ChartPointResponse(c.getName(), metricsService.averageScore(resultsByClass.getOrDefault(c.getId(), List.of()))))
                .toList();

        Map<Long, List<Result>> resultsBySubject = poolTermResults.stream()
                .collect(Collectors.groupingBy(r -> r.getSubject().getId()));
        List<ChartPointResponse> subjectComparison = subjectRepository.findAll().stream()
                .map(s -> new ChartPointResponse(s.getName(), metricsService.averageScore(resultsBySubject.getOrDefault(s.getId(), List.of()))))
                .toList();

        List<Term> allTermsOrdered = termRepository.findAllByOrderBySortOrderAsc();

        List<Result> poolSessionResults = poolIds.isEmpty() ? List.of()
                : resultRepository.findByStudentIdInAndAcademicSessionId(poolIds, session.getId());
        Map<Long, List<Result>> resultsByTerm = poolSessionResults.stream()
                .collect(Collectors.groupingBy(r -> r.getTerm().getId()));
        List<ChartPointResponse> performanceTrends = allTermsOrdered.stream()
                .map(t -> new ChartPointResponse(t.getName(), metricsService.averageScore(resultsByTerm.getOrDefault(t.getId(), List.of()))))
                .toList();

        List<Attendance> poolSessionAttendance = poolIds.isEmpty() ? List.of()
                : attendanceRepository.findByStudentIdInAndAcademicSessionId(poolIds, session.getId());
        Map<Long, List<Attendance>> attendanceByTerm = poolSessionAttendance.stream()
                .collect(Collectors.groupingBy(a -> a.getTerm().getId()));
        List<ChartPointResponse> attendanceTrends = allTermsOrdered.stream()
                .map(t -> new ChartPointResponse(t.getName(), metricsService.attendanceRate(attendanceByTerm.getOrDefault(t.getId(), List.of()))))
                .toList();

        List<Attendance> poolTermAttendance = poolIds.isEmpty() ? List.of()
                : attendanceRepository.findByStudentIdInAndAcademicSessionIdAndTermId(poolIds, session.getId(), term.getId());
        Map<Long, Double> avgMap = metricsService.averageScoreByStudent(poolTermResults);
        Map<Long, Double> attMap = metricsService.attendanceRateByStudent(poolTermAttendance);

        long low = 0, medium = 0, high = 0;
        for (Student s : pool) {
            String level = riskService.computeRisk(avgMap.getOrDefault(s.getId(), 0.0), attMap.getOrDefault(s.getId(), 0.0)).level();
            if (level.equals("LOW")) low++;
            else if (level.equals("MEDIUM")) medium++;
            else high++;
        }
        RiskCountsResponse riskCounts = new RiskCountsResponse(low, medium, high);

        return new AnalyticsResponse(classComparison, subjectComparison, performanceTrends, attendanceTrends, riskCounts);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentSummaryResponse> getStudentsAtRisk(Long sessionId, Long termId, List<Long> teacherClassIds) {
        AcademicSession session = periodResolver.resolveSession(sessionId);
        Term term = periodResolver.resolveTerm(termId);

        List<Student> pool = activePool(teacherClassIds);
        List<Long> poolIds = pool.stream().map(Student::getId).toList();
        List<Result> results = poolIds.isEmpty() ? List.of()
                : resultRepository.findByStudentIdInAndAcademicSessionIdAndTermId(poolIds, session.getId(), term.getId());
        List<Attendance> attendance = poolIds.isEmpty() ? List.of()
                : attendanceRepository.findByStudentIdInAndAcademicSessionIdAndTermId(poolIds, session.getId(), term.getId());
        Map<Long, Double> avgMap = metricsService.averageScoreByStudent(results);
        Map<Long, Double> attMap = metricsService.attendanceRateByStudent(attendance);

        return pool.stream()
                .map(s -> {
                    double avg = avgMap.getOrDefault(s.getId(), 0.0);
                    double att = attMap.getOrDefault(s.getId(), 0.0);
                    RiskInfo risk = riskService.computeRisk(avg, att);
                    return new StudentSummaryResponse(
                            s.getId(), s.getStudentCode(), s.getName(), s.getGender().name(),
                            s.getSchoolClass().getId(), s.getSchoolClass().getName(), s.getStatus().name(),
                            avg, att, risk);
                })
                .filter(s -> !s.risk().level().equals("LOW"))
                .sorted((a, b) -> {
                    if (a.risk().level().equals(b.risk().level())) return Double.compare(a.avgScore(), b.avgScore());
                    return a.risk().level().equals("HIGH") ? -1 : 1;
                })
                .toList();
    }

    private List<Student> activePool(List<Long> teacherClassIds) {
        List<Student> base = teacherClassIds != null
                ? studentRepository.findBySchoolClassIdIn(teacherClassIds)
                : studentRepository.findAll();
        return base.stream().filter(s -> s.getStatus() == RecordStatus.Active).toList();
    }
}
