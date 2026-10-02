package com.example.studentdashboard.service.impl;

import com.example.studentdashboard.entity.AcademicSession;
import com.example.studentdashboard.entity.Term;
import com.example.studentdashboard.exception.ResourceNotFoundException;
import com.example.studentdashboard.repository.AcademicSessionRepository;
import com.example.studentdashboard.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Internal helper (not a REST-facing service, so no interface) shared by
 * every service that accepts an optional sessionId/termId query param and
 * needs to default to whichever session/term is currently marked active.
 */
@Component
@RequiredArgsConstructor
public class AcademicPeriodResolver {

    private final AcademicSessionRepository sessionRepository;
    private final TermRepository termRepository;

    public AcademicSession resolveSession(Long sessionId) {
        if (sessionId != null) {
            return sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Academic session not found: " + sessionId));
        }
        return sessionRepository.findByCurrentTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "No academic session is marked current — set one via the admin tools or seed data."));
    }

    public Term resolveTerm(Long termId) {
        if (termId != null) {
            return termRepository.findById(termId)
                    .orElseThrow(() -> new ResourceNotFoundException("Term not found: " + termId));
        }
        return termRepository.findByCurrentTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "No term is marked current — set one via the admin tools or seed data."));
    }
}
