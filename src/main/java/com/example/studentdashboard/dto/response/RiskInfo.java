package com.example.studentdashboard.dto.response;

import java.util.List;

/**
 * level: LOW / MEDIUM / HIGH — badge: "Good" / "Needs Attention" / "At Risk".
 * Mirrors the frontend's exact risk-badge mapping so no UI changes are
 * needed for this shape once wired up.
 */
public record RiskInfo(
        String level,
        String badge,
        double avgScore,
        double attendanceRate,
        List<String> reasons
) {
}
