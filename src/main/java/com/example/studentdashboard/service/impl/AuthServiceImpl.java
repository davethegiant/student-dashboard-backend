package com.example.studentdashboard.service.impl;

import com.example.studentdashboard.dto.request.LoginRequest;
import com.example.studentdashboard.dto.request.RegisterUserRequest;
import com.example.studentdashboard.dto.response.LoginResponse;
import com.example.studentdashboard.dto.response.UserSummaryResponse;
import com.example.studentdashboard.entity.User;
import com.example.studentdashboard.exception.ConflictException;
import com.example.studentdashboard.repository.UserRepository;
import com.example.studentdashboard.security.JwtService;
import com.example.studentdashboard.security.Role;
import com.example.studentdashboard.security.UserPrincipal;
import com.example.studentdashboard.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public LoginResponse login(LoginRequest request) {
        // Resolve username-or-email to a concrete username WITHOUT revealing
        // which part was wrong on failure — always the same generic message.
        User user = userRepository.findByUsernameIgnoreCase(request.usernameOrEmail())
                .or(() -> userRepository.findByEmailIgnoreCase(request.usernameOrEmail()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username/email or password."));

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        String token = jwtService.generateToken(
                principal.getUserId(), principal.getUsername(), principal.getRole(), principal.getTeacherId());

        return new LoginResponse(token, toSummary(principal));
    }

    @Override
    public UserSummaryResponse me(UserPrincipal principal) {
        return toSummary(principal);
    }

    @Override
    @Transactional
    public UserSummaryResponse register(RegisterUserRequest request) {
        if (request.role() == Role.TEACHER) {
            throw new IllegalArgumentException(
                    "Teacher accounts must be created via the Teachers endpoints, not /api/auth/register, "
                            + "so the teacher profile and login stay linked.");
        }
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ConflictException("Username \"" + request.username() + "\" is already taken.");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email \"" + request.email() + "\" is already in use.");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(request.role())
                .build();
        user = userRepository.save(user);

        return new UserSummaryResponse(
                user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getRole().name(), null);
    }

    private UserSummaryResponse toSummary(UserPrincipal principal) {
        return new UserSummaryResponse(
                principal.getUserId(),
                principal.getUsername(),
                principal.getEmail(),
                principal.getFullName(),
                principal.getRole().name(),
                principal.getTeacherId());
    }
}
