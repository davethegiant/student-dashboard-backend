package com.example.studentdashboard.controller;

import com.example.studentdashboard.dto.response.MessageResponse;
import com.example.studentdashboard.dto.response.NotificationResponse;
import com.example.studentdashboard.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Attendance/academic risk alerts and system messages")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List all notifications, newest first")
    public ResponseEntity<List<NotificationResponse>> list() {
        return ResponseEntity.ok(notificationService.listNotifications());
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read")
    public ResponseEntity<MessageResponse> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ResponseEntity.ok(new MessageResponse("Notification marked as read."));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark every notification as read")
    public ResponseEntity<MessageResponse> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.ok(new MessageResponse("All notifications marked as read."));
    }
}
