package com.example.studentdashboard.dto.response;

import java.util.List;

/** Bootstrap endpoint — GET /api/meta — populates every dropdown/filter across the frontend. */
public record MetaResponse(
        List<SessionSummaryResponse> sessions,
        List<TermSummaryResponse> terms,
        String currentSession,
        String currentTerm,
        List<ClassSummaryResponse> classes,
        List<SubjectSummaryResponse> subjects
) {
}
