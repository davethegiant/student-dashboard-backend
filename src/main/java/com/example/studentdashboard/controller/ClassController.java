package com.example.studentdashboard.controller;

import com.example.studentdashboard.dto.request.ClassRequest;
import com.example.studentdashboard.dto.response.ClassDetailResponse;
import com.example.studentdashboard.dto.response.ClassOverviewResponse;
import com.example.studentdashboard.dto.response.ClassSummaryResponse;
import com.example.studentdashboard.dto.response.RosterStudentResponse;
import com.example.studentdashboard.security.TeacherScopeResolver;
import com.example.studentdashboard.security.UserPrincipal;
import com.example.studentdashboard.service.ClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@Tag(name = "Classes", description = "Class overview, detail, and attendance rosters — write operations are ADMIN only")
public class ClassController {

    private final ClassService classService;
    private final TeacherScopeResolver scopeResolver;

    @GetMapping
    @Operation(summary = "List classes", description = "TEACHER callers only see their own assigned classes.")
    public ResponseEntity<List<ClassOverviewResponse>> list(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long termId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> scope = scopeResolver.resolveClassScope(principal);
        return ResponseEntity.ok(classService.listClasses(sessionId, termId, scope));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Class detail — roster, per-student stats, top performer")
    public ResponseEntity<ClassDetailResponse> getDetail(
            @PathVariable Long id,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long termId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> scope = scopeResolver.resolveClassScope(principal);
        return ResponseEntity.ok(classService.getClassDetail(id, sessionId, termId, scope));
    }

    @GetMapping("/{id}/roster")
    @Operation(summary = "Active students in a class", description = "Lightweight shape for marking attendance.")
    public ResponseEntity<List<RosterStudentResponse>> getRoster(@PathVariable Long id) {
        return ResponseEntity.ok(classService.getRoster(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a class")
    public ResponseEntity<ClassSummaryResponse> create(@Valid @RequestBody ClassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classService.createClass(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a class")
    public ResponseEntity<ClassSummaryResponse> update(@PathVariable Long id, @Valid @RequestBody ClassRequest request) {
        return ResponseEntity.ok(classService.updateClass(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a class", description = "Fails with 409 if the class still has students assigned.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        classService.deleteClass(id);
        return ResponseEntity.noContent().build();
    }
}
