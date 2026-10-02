package com.example.studentdashboard.controller;

import com.example.studentdashboard.dto.response.AdminDashboardResponse;
import com.example.studentdashboard.dto.response.TeacherDashboardResponse;
import com.example.studentdashboard.security.UserPrincipal;
import com.example.studentdashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Role-specific landing dashboards")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "School-wide admin dashboard")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) Long classId) {
        return ResponseEntity.ok(dashboardService.getAdminDashboard(sessionId, termId, classId));
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Scoped dashboard for the authenticated teacher's own classes")
    public ResponseEntity<TeacherDashboardResponse> getTeacherDashboard(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long termId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dashboardService.getTeacherDashboard(principal.getTeacherId(), sessionId, termId));
    }
}
