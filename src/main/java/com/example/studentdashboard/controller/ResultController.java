package com.example.studentdashboard.controller;

import com.example.studentdashboard.dto.request.ResultRequest;
import com.example.studentdashboard.dto.response.PerformanceRecordResponse;
import com.example.studentdashboard.security.TeacherScopeResolver;
import com.example.studentdashboard.security.UserPrincipal;
import com.example.studentdashboard.service.ResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
@Tag(name = "Results", description = "CA + Exam scores — the frontend's \"Performance\" concept. Upserts by (student, subject, session, term).")
public class ResultController {

    private final ResultService resultService;
    private final TeacherScopeResolver scopeResolver;

    @GetMapping
    @Operation(summary = "Recent results, optionally filtered by class or subject")
    public ResponseEntity<List<PerformanceRecordResponse>> list(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> scope = scopeResolver.resolveClassScope(principal);
        return ResponseEntity.ok(resultService.listResults(classId, subjectId, sessionId, termId, limit, scope));
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "All results for one student in a session/term")
    public ResponseEntity<List<PerformanceRecordResponse>> listForStudent(
            @PathVariable Long studentId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long termId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> scope = scopeResolver.resolveClassScope(principal);
        return ResponseEntity.ok(resultService.listResultsForStudent(studentId, sessionId, termId, scope));
    }

    @PostMapping
    @Operation(summary = "Submit (or update) a CA + Exam score", description = "Auto-computes total and grade server-side.")
    public ResponseEntity<PerformanceRecordResponse> submit(
            @Valid @RequestBody ResultRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> scope = scopeResolver.resolveClassScope(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultService.submitResult(request, scope));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Correct an existing result's CA/Exam/comment")
    public ResponseEntity<PerformanceRecordResponse> update(
            @PathVariable Long id, @Valid @RequestBody ResultRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> scope = scopeResolver.resolveClassScope(principal);
        return ResponseEntity.ok(resultService.updateResult(id, request, scope));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a result")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> scope = scopeResolver.resolveClassScope(principal);
        resultService.deleteResult(id, scope);
        return ResponseEntity.noContent().build();
    }
}
