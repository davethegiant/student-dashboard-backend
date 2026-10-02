package com.example.studentdashboard.controller;

import com.example.studentdashboard.dto.request.CommentCreateRequest;
import com.example.studentdashboard.dto.request.StatusUpdateRequest;
import com.example.studentdashboard.dto.request.StudentRequest;
import com.example.studentdashboard.dto.response.CommentResponse;
import com.example.studentdashboard.dto.response.MessageResponse;
import com.example.studentdashboard.dto.response.StudentProfileResponse;
import com.example.studentdashboard.dto.response.StudentSummaryResponse;
import com.example.studentdashboard.entity.Gender;
import com.example.studentdashboard.entity.RecordStatus;
import com.example.studentdashboard.security.TeacherScopeResolver;
import com.example.studentdashboard.security.UserPrincipal;
import com.example.studentdashboard.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Student records, profiles, and teacher comments")
public class StudentController {

    private final StudentService studentService;
    private final TeacherScopeResolver scopeResolver;

    @GetMapping
    @Operation(summary = "Search/filter/paginate students", description = "TEACHER callers only ever see students in their own classes.")
    public ResponseEntity<Page<StudentSummaryResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) RecordStatus status,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long termId,
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        List<Long> scope = scopeResolver.resolveClassScope(principal);
        return ResponseEntity.ok(studentService.listStudents(
                search, classId, gender, status, riskLevel, sessionId, termId, scope, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Full student profile — performance, trend, attendance, risk, comments")
    public ResponseEntity<StudentProfileResponse> getProfile(
            @PathVariable Long id,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long termId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> scope = scopeResolver.resolveClassScope(principal);
        return ResponseEntity.ok(studentService.getProfile(id, sessionId, termId, scope));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a student")
    public ResponseEntity<StudentSummaryResponse> create(@Valid @RequestBody StudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a student's profile fields")
    public ResponseEntity<StudentSummaryResponse> update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate or deactivate a student")
    public ResponseEntity<MessageResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        studentService.updateStatus(id, request.status());
        return ResponseEntity.ok(new MessageResponse("Student status updated to " + request.status() + "."));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Permanently delete a student")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Add a teacher comment to a student", description = "TEACHER only — a comment is always attributed to a real teacher record.")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studentService.addComment(id, request, principal.getTeacherId()));
    }
}
