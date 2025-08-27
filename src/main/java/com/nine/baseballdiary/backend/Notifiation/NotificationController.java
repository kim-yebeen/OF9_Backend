package com.nine.baseballdiary.backend.Notifiation;

import com.nine.baseballdiary.backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

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

    // ✅ 1. 알림 목록 조회 (카테고리별 필터링 지원)
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getNotifications(
            @RequestParam(required = false, defaultValue = "ALL") String category) {

        Long userId = getCurrentUserId();
        List<NotificationDto> notifications = notificationService.getNotifications(userId, category);

        return ResponseEntity.ok(ApiResponse.success("알림 목록을 조회했습니다", notifications));
    }

    // ✅ 2. 팔로우 요청 처리
    @PostMapping("/follow-request/{requesterId}")
    public ResponseEntity<ApiResponse<Void>> handleFollowRequest(
            @PathVariable Long requesterId,
            @RequestParam boolean accept) {

        Long userId = getCurrentUserId();
        notificationService.handleFollowRequest(userId, requesterId, accept);

        String message = accept ? "팔로우 요청을 수락했습니다" : "팔로우 요청을 거절했습니다";
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    // ✅ 3. 시스템 알림 생성 (관리자용)
    @PostMapping("/system")
    public ResponseEntity<ApiResponse<Void>> createSystemNotification(@RequestBody SystemNotificationRequest request) {
        notificationService.createSystemNotification(request.getTitle(), request.getContent());

        return ResponseEntity.ok(ApiResponse.success("시스템 알림이 생성되었습니다"));
    }
}