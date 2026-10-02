package com.example.studentdashboard.repository;

import com.example.studentdashboard.entity.TeacherComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherCommentRepository extends JpaRepository<TeacherComment, Long> {

    List<TeacherComment> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}
