package com.example.studentdashboard.controller;

import com.example.studentdashboard.dto.response.AnalyticsResponse;
import com.example.studentdashboard.dto.response.StudentSummaryResponse;
import com.example.studentdashboard.security.TeacherScopeResolver;
import com.example.studentdashboard.security.UserPrincipal;
import com.example.studentdashboard.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Class/subject comparisons, trends, and risk breakdown — auto-scoped for TEACHER callers")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final TeacherScopeResolver scopeResolver;

    @GetMapping
    @Operation(summary = "Class/subject comparison, performance & attendance trends, risk counts")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long termId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> scope = scopeResolver.resolveClassScope(principal);
        return ResponseEntity.ok(analyticsService.getAnalytics(sessionId, termId, scope));
    }

    @GetMapping("/students-at-risk")
    @Operation(summary = "Every currently at-risk student (MEDIUM or HIGH), worst first")
    public ResponseEntity<List<StudentSummaryResponse>> getStudentsAtRisk(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long termId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> scope = scopeResolver.resolveClassScope(principal);
        return ResponseEntity.ok(analyticsService.getStudentsAtRisk(sessionId, termId, scope));
    }
}
