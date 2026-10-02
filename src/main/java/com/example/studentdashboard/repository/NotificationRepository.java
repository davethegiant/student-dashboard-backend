package com.example.studentdashboard.repository;

import com.example.studentdashboard.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByOrderByDateDesc();

    @Modifying
    @Query("update Notification n set n.read = true")
    void markAllRead();
}
