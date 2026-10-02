package com.example.studentdashboard.service.impl;

import com.example.studentdashboard.config.RiskThresholdProperties;
import com.example.studentdashboard.dto.response.RiskInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test — no Spring context. Directly covers the "analytics
 * calculations" the spec asks for: the risk engine ported from the
 * frontend (attendance < 75% or average < 50% -> flagged; both -> HIGH).
 */
class RiskServiceImplTest {

    private final RiskServiceImpl riskService = new RiskServiceImpl(new RiskThresholdProperties(75.0, 50.0));

    @Test
    void lowRisk_whenBothAboveThreshold() {
        RiskInfo risk = riskService.computeRisk(80.0, 90.0);
        assertThat(risk.level()).isEqualTo("LOW");
        assertThat(risk.badge()).isEqualTo("Good");
        assertThat(risk.reasons()).isEmpty();
    }

    @Test
    void mediumRisk_whenOnlyAttendanceBelowThreshold() {
        RiskInfo risk = riskService.computeRisk(80.0, 60.0);
        assertThat(risk.level()).isEqualTo("MEDIUM");
        assertThat(risk.badge()).isEqualTo("Needs Attention");
        assertThat(risk.reasons()).hasSize(1);
        assertThat(risk.reasons().get(0)).contains("Attendance");
    }

    @Test
    void mediumRisk_whenOnlyScoreBelowThreshold() {
        RiskInfo risk = riskService.computeRisk(40.0, 90.0);
        assertThat(risk.level()).isEqualTo("MEDIUM");
        assertThat(risk.badge()).isEqualTo("Needs Attention");
        assertThat(risk.reasons()).hasSize(1);
        assertThat(risk.reasons().get(0)).contains("Average score");
    }

    @Test
    void highRisk_whenBothBelowThreshold() {
        RiskInfo risk = riskService.computeRisk(44.0, 68.0);
        assertThat(risk.level()).isEqualTo("HIGH");
        assertThat(risk.badge()).isEqualTo("At Risk");
        assertThat(risk.reasons()).hasSize(2);
    }

    @Test
    void boundaryValues_exactlyAtThreshold_areNotFlagged() {
        // Strictly-less-than semantics: exactly at the threshold is fine, not flagged.
        RiskInfo risk = riskService.computeRisk(50.0, 75.0);
        assertThat(risk.level()).isEqualTo("LOW");
    }

    @Test
    void justBelowThreshold_isFlagged() {
        RiskInfo risk = riskService.computeRisk(49.9, 74.9);
        assertThat(risk.level()).isEqualTo("HIGH");
    }
}
