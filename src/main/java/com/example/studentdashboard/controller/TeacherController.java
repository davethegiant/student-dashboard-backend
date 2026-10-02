package com.example.studentdashboard.controller;

import com.example.studentdashboard.dto.request.StatusUpdateRequest;
import com.example.studentdashboard.dto.request.TeacherCreateRequest;
import com.example.studentdashboard.dto.request.TeacherLoginRequest;
import com.example.studentdashboard.dto.request.TeacherRequest;
import com.example.studentdashboard.dto.response.MessageResponse;
import com.example.studentdashboard.dto.response.TeacherSummaryResponse;
import com.example.studentdashboard.entity.RecordStatus;
import com.example.studentdashboard.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

/** Every endpoint here is ADMIN-only, matching the frontend's Teachers page (nav role: ["ADMIN"]). */
@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Teachers", description = "Teacher records, class/subject assignments, and login access (ADMIN only)")
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping
    @Operation(summary = "List teachers")
    public ResponseEntity<List<TeacherSummaryResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RecordStatus status) {
        return ResponseEntity.ok(teacherService.listTeachers(search, status));
    }

    @PostMapping
    @Operation(summary = "Create a teacher", description = "Optionally grants login access in the same call.")
    public ResponseEntity<TeacherSummaryResponse> create(@Valid @RequestBody TeacherCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teacherService.createTeacher(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a teacher's profile and class/subject assignments")
    public ResponseEntity<TeacherSummaryResponse> update(@PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
        return ResponseEntity.ok(teacherService.updateTeacher(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a teacher", description = "Deactivating immediately blocks their login, even for an already-issued token.")
    public ResponseEntity<MessageResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        teacherService.updateStatus(id, request.status());
        return ResponseEntity.ok(new MessageResponse("Teacher status updated to " + request.status() + "."));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Permanently delete a teacher")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/login")
    @Operation(summary = "Grant or reset a teacher's login access")
    public ResponseEntity<TeacherSummaryResponse> grantOrResetLogin(
            @PathVariable Long id, @Valid @RequestBody TeacherLoginRequest request) {
        return ResponseEntity.ok(teacherService.grantOrResetLogin(id, request));
    }

    @DeleteMapping("/{id}/login")
    @Operation(summary = "Revoke a teacher's login access without deleting their profile")
    public ResponseEntity<Void> revokeLogin(@PathVariable Long id) {
        teacherService.revokeLogin(id);
        return ResponseEntity.noContent().build();
    }
}
