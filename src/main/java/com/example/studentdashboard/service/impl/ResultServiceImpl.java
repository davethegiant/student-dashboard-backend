package com.example.studentdashboard.service.impl;

import com.example.studentdashboard.dto.request.ResultRequest;
import com.example.studentdashboard.dto.response.PerformanceRecordResponse;
import com.example.studentdashboard.entity.AcademicSession;
import com.example.studentdashboard.entity.Result;
import com.example.studentdashboard.entity.SchoolClass;
import com.example.studentdashboard.entity.Student;
import com.example.studentdashboard.entity.Subject;
import com.example.studentdashboard.entity.Term;
import com.example.studentdashboard.exception.ResourceNotFoundException;
import com.example.studentdashboard.exception.UnauthorizedScopeException;
import com.example.studentdashboard.repository.ResultRepository;
import com.example.studentdashboard.repository.StudentRepository;
import com.example.studentdashboard.repository.SubjectRepository;
import com.example.studentdashboard.service.ResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ResultServiceImpl implements ResultService {

    private final ResultRepository resultRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicPeriodResolver periodResolver;

    @Override
    @Transactional(readOnly = true)
    public List<PerformanceRecordResponse> listResults(Long classId, Long subjectId, Long sessionId, Long termId,
                                                         Integer limit, List<Long> teacherClassIds) {
        AcademicSession session = periodResolver.resolveSession(sessionId);
        Term term = periodResolver.resolveTerm(termId);

        List<Result> results;
        if (classId != null) {
            if (teacherClassIds != null && !teacherClassIds.contains(classId)) {
                throw new UnauthorizedScopeException("You can only view results for classes you teach.");
            }
            results = resultRepository.findBySchoolClassIdAndAcademicSessionIdAndTermId(classId, session.getId(), term.getId());
        } else if (subjectId != null) {
            results = scopeToTeacher(
                    resultRepository.findBySubjectIdAndAcademicSessionIdAndTermId(subjectId, session.getId(), term.getId()),
                    teacherClassIds);
        } else {
            results = scopeToTeacher(
                    resultRepository.findByAcademicSessionIdAndTermId(session.getId(), term.getId()),
                    teacherClassIds);
        }

        // Auto-increment id ordering approximates insertion order — newest first.
        results = results.stream().sorted(Comparator.comparing(Result::getId).reversed()).toList();
        if (limit != null && limit > 0 && results.size() > limit) {
            results = results.subList(0, limit);
        }

        return results.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PerformanceRecordResponse> listResultsForStudent(Long studentId, Long sessionId, Long termId, List<Long> teacherClassIds) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        if (teacherClassIds != null && !teacherClassIds.contains(student.getSchoolClass().getId())) {
            throw new UnauthorizedScopeException("You can only view results for students in classes you teach.");
        }
        AcademicSession session = periodResolver.resolveSession(sessionId);
        Term term = periodResolver.resolveTerm(termId);
        return resultRepository.findByStudentIdAndAcademicSessionIdAndTermId(studentId, session.getId(), term.getId())
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public PerformanceRecordResponse submitResult(ResultRequest request, List<Long> teacherClassIds) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + request.studentId()));
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.subjectId()));
        AcademicSession session = periodResolver.resolveSession(request.academicSessionId());
        Term term = periodResolver.resolveTerm(request.termId());

        SchoolClass studentClass = student.getSchoolClass();
        if (teacherClassIds != null && !teacherClassIds.contains(studentClass.getId())) {
            throw new UnauthorizedScopeException("You can only record results for students in classes you teach.");
        }

        int total = request.ca() + request.exam();
        String grade = Result.gradeFor(total);

        Result result = resultRepository
                .findByStudentIdAndSubjectIdAndAcademicSessionIdAndTermId(
                        student.getId(), subject.getId(), session.getId(), term.getId())
                .orElseGet(() -> Result.builder()
                        .student(student)
                        .subject(subject)
                        .academicSession(session)
                        .term(term)
                        .build());

        // Keep class in sync even on an update, in case the student transferred since the last entry.
        result.setSchoolClass(studentClass);
        result.setCa(request.ca());
        result.setExam(request.exam());
        result.setTotal(total);
        result.setGrade(grade);
        result.setComment(request.comment());

        result = resultRepository.save(result);
        return toResponse(result);
    }

    @Override
    @Transactional
    public PerformanceRecordResponse updateResult(Long resultId, ResultRequest request, List<Long> teacherClassIds) {
        Result result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found: " + resultId));

        if (teacherClassIds != null && !teacherClassIds.contains(result.getSchoolClass().getId())) {
            throw new UnauthorizedScopeException("You can only update results for classes you teach.");
        }

        int total = request.ca() + request.exam();
        result.setCa(request.ca());
        result.setExam(request.exam());
        result.setTotal(total);
        result.setGrade(Result.gradeFor(total));
        result.setComment(request.comment());

        result = resultRepository.save(result);
        return toResponse(result);
    }

    @Override
    @Transactional
    public void deleteResult(Long resultId, List<Long> teacherClassIds) {
        Result result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found: " + resultId));
        if (teacherClassIds != null && !teacherClassIds.contains(result.getSchoolClass().getId())) {
            throw new UnauthorizedScopeException("You can only delete results for classes you teach.");
        }
        resultRepository.delete(result);
    }

    private List<Result> scopeToTeacher(List<Result> results, List<Long> teacherClassIds) {
        if (teacherClassIds == null) return results;
        Set<Long> scope = new HashSet<>(teacherClassIds);
        return results.stream().filter(r -> scope.contains(r.getSchoolClass().getId())).toList();
    }

    private PerformanceRecordResponse toResponse(Result r) {
        return new PerformanceRecordResponse(
                r.getId(), r.getStudent().getName(), r.getSchoolClass().getName(), r.getSubject().getName(),
                r.getCa(), r.getExam(), r.getTotal(), r.getGrade());
    }
}
