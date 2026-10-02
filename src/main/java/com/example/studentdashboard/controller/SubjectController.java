package com.example.studentdashboard.controller;

import com.example.studentdashboard.dto.request.SubjectRequest;
import com.example.studentdashboard.dto.response.SubjectOverviewResponse;
import com.example.studentdashboard.dto.response.SubjectSummaryResponse;
import com.example.studentdashboard.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

/** Every endpoint here is ADMIN-only, matching the frontend's Subjects page (nav role: ["ADMIN"]). */
@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Subjects", description = "Curriculum subjects and their performance comparison (ADMIN only)")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @Operation(summary = "List subjects with performance overview")
    public ResponseEntity<List<SubjectOverviewResponse>> list(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long termId) {
        return ResponseEntity.ok(subjectService.listSubjects(sessionId, termId));
    }

    @PostMapping
    @Operation(summary = "Create a subject")
    public ResponseEntity<SubjectSummaryResponse> create(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.createSubject(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a subject")
    public ResponseEntity<SubjectSummaryResponse> update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(subjectService.updateSubject(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a subject", description = "Fails with 409 if the subject still has results attached.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }
}
