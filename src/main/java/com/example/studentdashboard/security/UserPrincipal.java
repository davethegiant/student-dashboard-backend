package com.example.studentdashboard.security;

import com.example.studentdashboard.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security's UserDetails adapter around our User entity.
 * teacherId is populated by JwtAuthenticationFilter from the token's own
 * claim (not re-derived here) so every downstream service can scope
 * TEACHER requests without an extra query.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String email;
    private final String fullName;
    private final String passwordHash;
    private final Role role;
    private final Long teacherId;
    private final boolean enabled;

    public UserPrincipal(User user, Long teacherId, boolean enabled) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.passwordHash = user.getPassword();
        this.role = user.getRole();
        this.teacherId = teacherId;
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        // Needed by DaoAuthenticationProvider during the initial
        // /api/auth/login check. On every later request, JwtAuthenticationFilter
        // builds the Authentication directly from an already-verified token
        // and never re-checks this value.
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // false for a TEACHER whose Teacher.status has been set to Inactive —
        // both DaoAuthenticationProvider (at login) and JwtAuthenticationFilter
        // (on every later request) reject disabled principals.
        return enabled;
    }
}
