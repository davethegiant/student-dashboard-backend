package com.example.studentdashboard.service;

import com.example.studentdashboard.dto.response.AttendanceBreakdownResponse;
import com.example.studentdashboard.entity.Attendance;
import com.example.studentdashboard.entity.Result;

import java.util.List;
import java.util.Map;

/**
 * Pure computation over already-fetched Result/Attendance batches — no
 * repository access here. Callers fetch the right scoped slice ONCE (e.g.
 * "every Result for this class+session+term") and pass it in, so
 * class-/school-wide aggregates cost one query instead of one-per-student.
 */
public interface AcademicMetricsService {

    double averageScore(List<Result> results);

    double attendanceRate(List<Attendance> records);

    AttendanceBreakdownResponse attendanceBreakdown(List<Attendance> records);

    /** Keyed by Student.id — groups a batch before averaging each student's own results. */
    Map<Long, Double> averageScoreByStudent(List<Result> results);

    /** Keyed by Student.id. */
    Map<Long, Double> attendanceRateByStudent(List<Attendance> records);
}
