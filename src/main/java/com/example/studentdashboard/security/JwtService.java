package com.example.studentdashboard.security;

import com.example.studentdashboard.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Issues and validates HS256 JWTs. Claims embed just enough context
 * (userId, role, teacherId) that request-scoped authorization doesn't
 * need an extra DB round-trip per request beyond loading the User itself
 * in {@link JwtAuthenticationFilter}.
 */
@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, Role role, Long teacherId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.expirationMinutes(), ChronoUnit.MINUTES);

        var builder = Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry));

        if (teacherId != null) {
            builder.claim("teacherId", teacherId);
        }

        return builder.signWith(signingKey).compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(Claims claims) {
        Object raw = claims.get("userId");
        return raw == null ? null : Long.valueOf(raw.toString());
    }

    public Long extractTeacherId(Claims claims) {
        Object raw = claims.get("teacherId");
        return raw == null ? null : Long.valueOf(raw.toString());
    }

    public Role extractRole(Claims claims) {
        return Role.valueOf((String) claims.get("role"));
    }
}
