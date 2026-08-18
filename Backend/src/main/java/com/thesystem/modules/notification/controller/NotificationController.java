package com.thesystem.modules.notification.controller;

import com.thesystem.common.response.ApiResponse;
import com.thesystem.modules.notification.dto.NotificationResponse;
import com.thesystem.modules.notification.service.NotificationService;
import com.thesystem.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List notifications for the authenticated user")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> listNotifications() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<NotificationResponse> notifications = notificationService.getNotifications(userId);
        return ResponseEntity.ok(ApiResponse.ok(notifications, "Notifications retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single notification")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(
            @PathVariable @Parameter(description = "Notification ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        NotificationResponse notification = notificationService.getNotification(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(notification, "Notification retrieved successfully", UUID.randomUUID().toString()));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable @Parameter(description = "Notification ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        NotificationResponse notification = notificationService.markAsRead(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(notification, "Notification marked as read", UUID.randomUUID().toString()));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        UUID userId = SecurityUtils.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "All notifications marked as read", UUID.randomUUID().toString()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete/dismiss a notification")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable @Parameter(description = "Notification ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        notificationService.deleteNotification(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Notification deleted successfully", UUID.randomUUID().toString()));
    }
}
