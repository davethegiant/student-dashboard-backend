package com.example.studentdashboard.service;

import com.example.studentdashboard.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {

    List<NotificationResponse> listNotifications();

    void markRead(Long notificationId);

    void markAllRead();
}
