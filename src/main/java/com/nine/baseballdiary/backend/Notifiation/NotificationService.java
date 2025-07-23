package com.nine.baseballdiary.backend.Notifiation;

import com.nine.baseballdiary.backend.record.Record; // ✅ 명시적 import 추가
import com.nine.baseballdiary.backend.record.RecordRepository; // ✅ 추가
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.repository.UserFollowRepository;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final UserFollowRepository userFollowRepo;
    private final RecordRepository recordRepo;

    // 공감 매핑 정보
    private static final Map<Integer, String> EMOTION_MAP = Map.of(
            1, "짜릿해요", 2, "만족해요", 3, "감동이에요", 4, "놀랐어요", 5, "행복해요",
            6, "답답해요", 7, "아쉬워요", 8, "화났어요", 9, "지쳤어요"
    );

    private String getEmotionName(Integer code) {
        return EMOTION_MAP.getOrDefault(code, "공감");
    }
    // 1. 공감 알림 생성
    public void createReactionNotification(Long recordOwnerId, Long reactorId, Long recordId, Integer reactionTypeId) {
        if (recordOwnerId.equals(reactorId)) return;

        User reactor = userRepo.findById(reactorId).orElseThrow();
        String emotionName = getEmotionName(reactionTypeId);

        Notification notification = Notification.builder()
                .userId(recordOwnerId)
                .type(NotificationType.REACTION)
                .title("공감 알림") // title은 간단히
                .content(reactor.getNickname() + "님이 나의 직관기록에 " + emotionName + " 이모지를 남겼어요") // ✅ 수정된 content
                .relatedUserId(reactorId)
                .relatedRecordId(recordId)
                .reactionTypeId(reactionTypeId)
                .isRead(false)
                .build();

        notificationRepo.save(notification);
    }

    // 팔로우 알림
    public void createFollowNotification(Long followeeId, Long followerId) {
        User follower = userRepo.findById(followerId).orElseThrow();

        Notification notification = Notification.builder()
                .userId(followeeId)
                .type(NotificationType.FOLLOW)
                .title("팔로우 알림")
                .content(follower.getNickname() + "님이 팔로우를 시작했어요") // ✅ 간단하게 수정
                .relatedUserId(followerId)
                .isRead(false)
                .build();

        notificationRepo.save(notification);
    }

    // 팔로우 요청 알림
    public void createFollowRequestNotification(Long targetId, Long requesterId) {
        User requester = userRepo.findById(requesterId).orElseThrow();

        Notification notification = Notification.builder()
                .userId(targetId)
                .type(NotificationType.FOLLOW_REQUEST)
                .title("팔로우 요청")
                .content(requester.getNickname() + "님이 팔로우를 요청했어요") // ✅ 간단하게 수정
                .relatedUserId(requesterId)
                .isRead(false)
                .build();

        notificationRepo.save(notification);
    }

    // 새 게시글 알림
    public void createNewRecordNotification(Long recordOwnerId, Long recordId) {
        User recordOwner = userRepo.findById(recordOwnerId).orElseThrow();

        List<Long> followerIds = userFollowRepo.findByFolloweeId_Id(recordOwnerId)
                .stream()
                .map(follow -> follow.getFollowerId().getId())
                .collect(Collectors.toList());

        List<Notification> notifications = followerIds.stream()
                .map(followerId -> Notification.builder()
                        .userId(followerId)
                        .type(NotificationType.NEW_RECORD)
                        .title("새 게시글")
                        .content(recordOwner.getNickname() + "님이 새로운 직관 기록을 작성했어요") // ✅ 간단하게 수정
                        .relatedUserId(recordOwnerId)
                        .relatedRecordId(recordId)
                        .isRead(false)
                        .build())
                .collect(Collectors.toList());

        notificationRepo.saveAll(notifications);
    }

    // 4. 시스템 소식 생성
    public void createSystemNotification(String title, String content) {
        List<User> allUsers = userRepo.findAll();

        List<Notification> systemNotifications = allUsers.stream()
                .map(user -> Notification.builder()
                        .userId(user.getId())
                        .type(NotificationType.SYSTEM)
                        .title(title)
                        .content(content)
                        .build())
                .collect(Collectors.toList());

        notificationRepo.saveAll(systemNotifications);
    }

    // 5. 알림 목록 조회
    public List<NotificationDto> getNotifications(Long userId, String type, Boolean isRead) {
        List<Notification> notifications;

        if (type != null && !type.equals("ALL")) {
            NotificationType notificationType = NotificationType.valueOf(type.toUpperCase());
            notifications = notificationRepo.findByUserIdAndTypeOrderByCreatedAtDesc(userId, notificationType);
        } else {
            notifications = notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
        }

        return notifications.stream()
                .filter(n -> isRead == null || n.getIsRead().equals(isRead))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 6. 읽지 않은 알림 개수
    public Long getUnreadCount(Long userId) {
        return notificationRepo.countByUserIdAndIsReadFalse(userId);
    }

    // 7. 알림 읽음 처리
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepo.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다"));
        notification.setIsRead(true);
    }

    // NotificationService.java의 convertToDto 메서드 수정
    private NotificationDto convertToDto(Notification notification) {
        NotificationDto dto = NotificationDto.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                // .title(notification.getTitle()) // ✅ title 제거
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .timeAgo(formatTimeAgo(notification.getCreatedAt())) // ✅ "3분 전" 형태
                .createdAt(notification.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .relatedRecordId(notification.getRelatedRecordId())
                .build();

        // ✅ 사용자 정보 설정 (닉네임, 프로필 이미지)
        if (notification.getRelatedUserId() != null) {
            userRepo.findById(notification.getRelatedUserId()).ifPresent(user -> {
                dto.setUserNickname(user.getNickname());
                dto.setUserProfileImage(user.getProfileImageUrl());
            });
        } else if (notification.getType() == NotificationType.SYSTEM) {
            dto.setUserNickname("LookIT");
            dto.setUserProfileImage("/images/lookit-logo.png");
        }

        // ✅ 공감 타입 정보 추가
        if (notification.getReactionTypeId() != null) {
            dto.setEmotionName(getEmotionName(notification.getReactionTypeId()));
            dto.setEmotionCode(notification.getReactionTypeId());
        }

        return dto;
    }

    private String formatTimeAgo(LocalDateTime createdAt) {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(createdAt, now);

        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (minutes < 1) return "방금 전";
        if (minutes < 60) return minutes + "분 전";
        if (hours < 24) return hours + "시간 전";
        if (days < 7) return days + "일 전";

        // 1주일 이상은 명확한 날짜 표시
        if (days < 365) {
            return createdAt.format(DateTimeFormatter.ofPattern("M월 d일"));
        } else {
            return createdAt.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"));
        }
    }
}
