package com.example.studentdashboard.service.impl;

import com.example.studentdashboard.config.RiskThresholdProperties;
import com.example.studentdashboard.dto.response.RiskInfo;
import com.example.studentdashboard.service.RiskService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RiskServiceImpl implements RiskService {

    private final RiskThresholdProperties thresholds;

    public RiskServiceImpl(RiskThresholdProperties thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public RiskInfo computeRisk(double avgScore, double attendanceRate) {
        boolean academicRisk = avgScore < thresholds.scoreThreshold();
        boolean attendanceRisk = attendanceRate < thresholds.attendanceThreshold();

        List<String> reasons = new ArrayList<>();
        if (academicRisk) {
            reasons.add(String.format(Locale.US, "Average score is %.1f%%, below the %.0f%% threshold.",
                    avgScore, thresholds.scoreThreshold()));
        }
        if (attendanceRisk) {
            reasons.add(String.format(Locale.US, "Attendance is %.1f%%, below the %.0f%% threshold.",
                    attendanceRate, thresholds.attendanceThreshold()));
        }

        String level;
        if (academicRisk && attendanceRisk) {
            level = "HIGH";
        } else if (academicRisk || attendanceRisk) {
            level = "MEDIUM";
        } else {
            level = "LOW";
        }

        return new RiskInfo(level, badgeFor(level), round1(avgScore), round1(attendanceRate), reasons);
    }

    @Override
    public String badgeFor(String level) {
        return switch (level) {
            case "HIGH" -> "At Risk";
            case "MEDIUM" -> "Needs Attention";
            default -> "Good";
        };
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
