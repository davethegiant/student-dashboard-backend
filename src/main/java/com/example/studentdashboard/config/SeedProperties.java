package com.example.studentdashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * adminUsername/adminEmail/adminPassword back the seeded ADMIN account
 * (see DemoDataSeedService#seedAdmin) — overridable via ADMIN_USERNAME,
 * ADMIN_EMAIL and ADMIN_PASSWORD so production doesn't ship the dev default.
 */
@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(boolean enabled, String adminUsername, String adminEmail, String adminPassword) {
}
