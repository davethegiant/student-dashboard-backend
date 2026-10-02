package com.example.studentdashboard.service;

import com.example.studentdashboard.dto.response.RiskInfo;

public interface RiskService {

    /**
     * attendance < threshold -> attendance risk; avgScore < threshold ->
     * academic risk; both -> HIGH; either -> MEDIUM; neither -> LOW.
     * Mirrors the frontend's risk engine exactly.
     */
    RiskInfo computeRisk(double avgScore, double attendanceRate);

    /** "At Risk" / "Needs Attention" / "Good" — the frontend's exact badge labels for a given level. */
    String badgeFor(String level);
}
