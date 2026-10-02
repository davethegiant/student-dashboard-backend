package com.example.studentdashboard.repository;

import com.example.studentdashboard.entity.ActivityLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogEntryRepository extends JpaRepository<ActivityLogEntry, Long> {

    List<ActivityLogEntry> findTop10ByOrderByCreatedAtDesc();
}
