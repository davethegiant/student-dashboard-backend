package com.example.studentdashboard.service.impl;

import com.example.studentdashboard.dto.response.ClassSummaryResponse;
import com.example.studentdashboard.dto.response.MetaResponse;
import com.example.studentdashboard.dto.response.SessionSummaryResponse;
import com.example.studentdashboard.dto.response.SubjectSummaryResponse;
import com.example.studentdashboard.dto.response.TermSummaryResponse;
import com.example.studentdashboard.entity.AcademicSession;
import com.example.studentdashboard.entity.Term;
import com.example.studentdashboard.repository.AcademicSessionRepository;
import com.example.studentdashboard.repository.SchoolClassRepository;
import com.example.studentdashboard.repository.SubjectRepository;
import com.example.studentdashboard.repository.TermRepository;
import com.example.studentdashboard.service.MetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetaServiceImpl implements MetaService {

    private final AcademicSessionRepository academicSessionRepository;
    private final TermRepository termRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;

    @Override
    @Transactional(readOnly = true)
    public MetaResponse getMeta() {
        List<SessionSummaryResponse> sessions = academicSessionRepository.findAll().stream()
                .map(s -> new SessionSummaryResponse(s.getId(), s.getLabel())).toList();
        List<TermSummaryResponse> terms = termRepository.findAllByOrderBySortOrderAsc().stream()
                .map(t -> new TermSummaryResponse(t.getId(), t.getName())).toList();
        String currentSession = academicSessionRepository.findByCurrentTrue()
                .map(AcademicSession::getLabel).orElse(null);
        String currentTerm = termRepository.findByCurrentTrue()
                .map(Term::getName).orElse(null);
        List<ClassSummaryResponse> classes = schoolClassRepository.findAll().stream()
                .map(c -> new ClassSummaryResponse(c.getId(), c.getClassCode(), c.getName(), c.getLevel()))
                .toList();
        List<SubjectSummaryResponse> subjects = subjectRepository.findAll().stream()
                .map(s -> new SubjectSummaryResponse(s.getId(), s.getSubjectCode(), s.getName()))
                .toList();

        return new MetaResponse(sessions, terms, currentSession, currentTerm, classes, subjects);
    }
}
