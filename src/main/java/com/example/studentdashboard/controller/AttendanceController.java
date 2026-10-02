package com.example.studentdashboard.controller;

import com.example.studentdashboard.dto.request.AttendanceBulkSubmitRequest;
import com.example.studentdashboard.dto.request.AttendanceUpdateRequest;
import com.example.studentdashboard.dto.response.AttendanceRecordResponse;
import com.example.studentdashboard.dto.response.AttendanceSubmitSummaryResponse;
import com.example.studentdashboard.security.TeacherScopeResolver;
import com.example.studentdashboard.security.UserPrincipal;
import com.example.studentdashboard.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Roster-based attendance marking and history")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final TeacherScopeResolver scopeResolver;

    @GetMapping
    @Operation(summary = "Attendance records, optionally filtered by class/date")
    public ResponseEntity<List<AttendanceRecordResponse>> list(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> scope = scopeResolver.resolveClassScope(principal);
        return ResponseEntity.ok(attendanceService.listAttendance(classId, sessionId, termId, date, scope));
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "Attendance history for one student")
    public ResponseEntity<List<AttendanceRecordResponse>> listForStudent(
            @PathVariable Long studentId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long termId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> scope = scopeResolver.resolveClassScope(principal);
        return ResponseEntity.ok(attendanceService.listForStudent(studentId, sessionId, termId, scope));
    }

    @PostMapping
    @Operation(summary = "Submit a whole class's attendance for a date", description = "Mirrors the frontend's roster-marking flow — Present/Absent/Late per student.")
    public ResponseEntity<AttendanceSubmitSummaryResponse> submitBulk(
            @Valid @RequestBody AttendanceBulkSubmitRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> scope = scopeResolver.resolveClassScope(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.submitBulk(request, scope));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Correct a single attendance record")
    public ResponseEntity<AttendanceRecordResponse> update(
            @PathVariable Long id, @Valid @RequestBody AttendanceUpdateRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> scope = scopeResolver.resolveClassScope(principal);
        return ResponseEntity.ok(attendanceService.updateAttendance(id, request, scope));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an attendance record")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> scope = scopeResolver.resolveClassScope(principal);
        attendanceService.deleteAttendance(id, scope);
        return ResponseEntity.noContent().build();
    }
}
