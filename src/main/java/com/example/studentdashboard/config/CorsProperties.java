package com.example.studentdashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Bound from app.cors.allowed-origins. Spring Boot's relaxed binding
 * automatically splits a comma-separated string (e.g. from a single
 * CORS_ALLOWED_ORIGINS env var) into this List<String>.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
