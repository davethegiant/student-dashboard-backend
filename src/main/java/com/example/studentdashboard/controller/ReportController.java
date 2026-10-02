package com.example.studentdashboard.controller;

import com.example.studentdashboard.dto.request.ReportGenerateRequest;
import com.example.studentdashboard.dto.response.ReportResponse;
import com.example.studentdashboard.security.TeacherScopeResolver;
import com.example.studentdashboard.security.UserPrincipal;
import com.example.studentdashboard.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "5 report types (student/class/attendance/at_risk/subject) — authorized server-side for TEACHER callers")
public class ReportController {

    private final ReportService reportService;
    private final TeacherScopeResolver scopeResolver;

    @PostMapping("/generate")
    @Operation(summary = "Generate a report", description = "A TEACHER requesting a class/student outside their assignment gets a \"not authorized\" report body, not the data.")
    public ResponseEntity<ReportResponse> generate(
            @Valid @RequestBody ReportGenerateRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> scope = scopeResolver.resolveClassScope(principal);
        return ResponseEntity.ok(reportService.generateReport(request, scope));
    }
}
