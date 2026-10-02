package com.example.studentdashboard.service.impl;

import com.example.studentdashboard.dto.response.AttendanceBreakdownResponse;
import com.example.studentdashboard.entity.Attendance;
import com.example.studentdashboard.entity.AttendanceStatus;
import com.example.studentdashboard.entity.Result;
import com.example.studentdashboard.service.AcademicMetricsService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AcademicMetricsServiceImpl implements AcademicMetricsService {

    @Override
    public double averageScore(List<Result> results) {
        if (results.isEmpty()) return 0.0;
        double sum = results.stream().mapToInt(Result::getTotal).sum();
        return round1(sum / results.size());
    }

    @Override
    public double attendanceRate(List<Attendance> records) {
        if (records.isEmpty()) return 0.0;
        long present = records.stream().filter(a -> a.getStatus() == AttendanceStatus.Present).count();
        long late = records.stream().filter(a -> a.getStatus() == AttendanceStatus.Late).count();
        double rate = ((present + late * 0.5) / records.size()) * 100.0;
        return round1(rate);
    }

    @Override
    public AttendanceBreakdownResponse attendanceBreakdown(List<Attendance> records) {
        int present = (int) records.stream().filter(a -> a.getStatus() == AttendanceStatus.Present).count();
        int absent = (int) records.stream().filter(a -> a.getStatus() == AttendanceStatus.Absent).count();
        int late = (int) records.stream().filter(a -> a.getStatus() == AttendanceStatus.Late).count();
        return new AttendanceBreakdownResponse(present, absent, late, records.size());
    }

    @Override
    public Map<Long, Double> averageScoreByStudent(List<Result> results) {
        Map<Long, List<Result>> byStudent = results.stream()
                .collect(Collectors.groupingBy(r -> r.getStudent().getId()));
        return byStudent.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> averageScore(e.getValue())));
    }

    @Override
    public Map<Long, Double> attendanceRateByStudent(List<Attendance> records) {
        Map<Long, List<Attendance>> byStudent = records.stream()
                .collect(Collectors.groupingBy(a -> a.getStudent().getId()));
        return byStudent.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> attendanceRate(e.getValue())));
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
