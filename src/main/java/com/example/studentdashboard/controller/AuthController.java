package com.example.studentdashboard.controller;

import com.example.studentdashboard.dto.request.LoginRequest;
import com.example.studentdashboard.dto.request.RegisterUserRequest;
import com.example.studentdashboard.dto.response.LoginResponse;
import com.example.studentdashboard.dto.response.UserSummaryResponse;
import com.example.studentdashboard.security.UserPrincipal;
import com.example.studentdashboard.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Login, session identity, and admin account creation")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Log in with username or email + password", description = "Public — no token required.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new ADMIN account", description = "ADMIN only. Teacher accounts are created via /api/teachers instead.")
    public ResponseEntity<UserSummaryResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Return the currently authenticated user")
    public ResponseEntity<UserSummaryResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.me(principal));
    }
}
