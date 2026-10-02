package com.example.studentdashboard.service;

import com.example.studentdashboard.dto.request.LoginRequest;
import com.example.studentdashboard.dto.request.RegisterUserRequest;
import com.example.studentdashboard.dto.response.LoginResponse;
import com.example.studentdashboard.dto.response.UserSummaryResponse;
import com.example.studentdashboard.security.UserPrincipal;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    UserSummaryResponse me(UserPrincipal principal);

    /**
     * ADMIN-only. Only permits creating ADMIN accounts — a TEACHER-role
     * account created this way would have no linked Teacher record and
     * could never actually log in (see AppUserDetailsService). Real
     * teacher accounts go through TeacherService instead, which keeps
     * Teacher <-> User consistent.
     */
    UserSummaryResponse register(RegisterUserRequest request);
}
