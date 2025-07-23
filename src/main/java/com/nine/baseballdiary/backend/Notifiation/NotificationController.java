package com.nine.baseballdiary.backend.Notifiation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자가 아닙니다.");
        }
        return Long.parseLong((String) authentication.getPrincipal());
    }

    // 1. 알림 목록 조회
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications(
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(required = false) Boolean isRead) {
        Long userId = getCurrentUserId();
        List<NotificationDto> notifications = notificationService.getNotifications(userId, type, isRead);
        return ResponseEntity.ok(notifications);
    }

    // 2. 읽지 않은 알림 개수
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        Long userId = getCurrentUserId();
        Long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // 3. 알림 읽음 처리
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        Long userId = getCurrentUserId();
        notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok().build();
    }

    // 4. 시스템 알림 생성 (관리자용)
    @PostMapping("/system")
    public ResponseEntity<Void> createSystemNotification(@RequestBody SystemNotificationRequest request) {
        notificationService.createSystemNotification(request.getTitle(), request.getContent());
        return ResponseEntity.status(201).build();
    }
}