package com.example.studentdashboard.service.impl;

import com.example.studentdashboard.dto.request.SubjectRequest;
import com.example.studentdashboard.dto.response.SubjectOverviewResponse;
import com.example.studentdashboard.dto.response.SubjectSummaryResponse;
import com.example.studentdashboard.entity.AcademicSession;
import com.example.studentdashboard.entity.Result;
import com.example.studentdashboard.entity.Subject;
import com.example.studentdashboard.entity.Term;
import com.example.studentdashboard.exception.ConflictException;
import com.example.studentdashboard.exception.ResourceNotFoundException;
import com.example.studentdashboard.repository.ResultRepository;
import com.example.studentdashboard.repository.SubjectRepository;
import com.example.studentdashboard.repository.TeacherRepository;
import com.example.studentdashboard.service.AcademicMetricsService;
import com.example.studentdashboard.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final ResultRepository resultRepository;
    private final TeacherRepository teacherRepository;
    private final AcademicMetricsService metricsService;
    private final AcademicPeriodResolver periodResolver;

    @Override
    @Transactional(readOnly = true)
    public List<SubjectOverviewResponse> listSubjects(Long sessionId, Long termId) {
        AcademicSession session = periodResolver.resolveSession(sessionId);
        Term term = periodResolver.resolveTerm(termId);
        return subjectRepository.findAll().stream().map(s -> toOverview(s, session, term)).toList();
    }

    @Override
    @Transactional
    public SubjectSummaryResponse createSubject(SubjectRequest request) {
        if (subjectRepository.findBySubjectCode(request.subjectCode()).isPresent()) {
            throw new ConflictException("Subject code \"" + request.subjectCode() + "\" is already in use.");
        }
        Subject subject = Subject.builder().subjectCode(request.subjectCode()).name(request.name()).build();
        subject = subjectRepository.save(subject);
        return toSummary(subject);
    }

    @Override
    @Transactional
    public SubjectSummaryResponse updateSubject(Long subjectId, SubjectRequest request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));

        boolean codeChanged = !subject.getSubjectCode().equalsIgnoreCase(request.subjectCode());
        if (codeChanged && subjectRepository.findBySubjectCode(request.subjectCode()).isPresent()) {
            throw new ConflictException("Subject code \"" + request.subjectCode() + "\" is already in use.");
        }

        subject.setSubjectCode(request.subjectCode());
        subject.setName(request.name());
        subject = subjectRepository.save(subject);
        return toSummary(subject);
    }

    @Override
    @Transactional
    public void deleteSubject(Long subjectId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject not found: " + subjectId);
        }
        // No cascade from result.subject_id on purpose — deleting a subject
        // that still has results attached correctly fails via FK constraint,
        // surfaced as a clean 409 by GlobalExceptionHandler.
        subjectRepository.deleteById(subjectId);
    }

    private SubjectOverviewResponse toOverview(Subject s, AcademicSession session, Term term) {
        List<Result> results = resultRepository.findBySubjectIdAndAcademicSessionIdAndTermId(s.getId(), session.getId(), term.getId());
        double avg = metricsService.averageScore(results);
        long classCount = results.stream().map(r -> r.getSchoolClass().getId()).distinct().count();
        long teacherCount = teacherRepository.countBySubjectId(s.getId());
        return new SubjectOverviewResponse(s.getId(), s.getSubjectCode(), s.getName(), avg, (int) classCount, (int) teacherCount);
    }

    private SubjectSummaryResponse toSummary(Subject s) {
        return new SubjectSummaryResponse(s.getId(), s.getSubjectCode(), s.getName());
    }
}
