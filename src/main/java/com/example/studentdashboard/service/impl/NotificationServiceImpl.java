package com.example.studentdashboard.service.impl;

import com.example.studentdashboard.dto.response.NotificationResponse;
import com.example.studentdashboard.entity.Notification;
import com.example.studentdashboard.exception.ResourceNotFoundException;
import com.example.studentdashboard.repository.NotificationRepository;
import com.example.studentdashboard.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> listNotifications() {
        return notificationRepository.findAllByOrderByDateDesc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void markRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllRead() {
        notificationRepository.markAllRead();
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.getDate().toString(), n.isRead());
    }
}
