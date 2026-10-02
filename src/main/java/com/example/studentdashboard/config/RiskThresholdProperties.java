package com.example.studentdashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Defaults (75% attendance, 50% average) match the existing frontend's
 * hardcoded risk logic exactly. Overridable via app.risk.* so the
 * thresholds don't require a code change/redeploy to adjust.
 */
@ConfigurationProperties(prefix = "app.risk")
public record RiskThresholdProperties(
        double attendanceThreshold,
        double scoreThreshold
) {
    public RiskThresholdProperties {
        if (attendanceThreshold <= 0) attendanceThreshold = 75.0;
        if (scoreThreshold <= 0) scoreThreshold = 50.0;
    }
}
