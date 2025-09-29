package com.nine.baseballdiary.backend.Notifiation;

import com.nine.baseballdiary.backend.record.GameRecordRepository;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.repository.UserFollowRepository;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    private final GameRecordRepository recordRepo;

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
                .title("반응 공감")
                .content(reactor.getNickname() + "님이 나의 직관기록에 " + emotionName + " 반응을 남겼어요")
                .relatedUserId(reactorId)
                .relatedRecordId(recordId)
                .reactionTypeId(reactionTypeId)
                .isRead(false)
                .build();

        notificationRepo.save(notification);
    }

    // 2. 팔로우 알림
    public void createFollowNotification(Long followeeId, Long followerId) {
        User follower = userRepo.findById(followerId).orElseThrow();

        Notification notification = Notification.builder()
                .userId(followeeId)
                .type(NotificationType.FOLLOW)
                .title("친구의 직관기록")
                .content(follower.getNickname() + "님이 나를 팔로우 했어요")
                .relatedUserId(followerId)
                .isRead(false)
                .build();

        notificationRepo.save(notification);
    }

    // 3. 팔로우 요청 알림
    public void createFollowRequestNotification(Long targetId, Long requesterId) {
        User requester = userRepo.findById(requesterId).orElseThrow();

        Notification notification = Notification.builder()
                .userId(targetId)
                .type(NotificationType.FOLLOW_REQUEST)
                .title("친구의 직관기록")
                .content(requester.getNickname() + "님의 팔로우 요청")
                .relatedUserId(requesterId)
                .isRead(false)
                .build();

        notificationRepo.save(notification);
    }

    // 4. 새 게시글 알림
    public void createNewRecordNotification(Long recordOwnerId, Long recordId) {
        User recordOwner = userRepo.findById(recordOwnerId).orElseThrow();

        List<Long> followerIds = userFollowRepo.findByFollowee_Id(recordOwnerId)
                .stream()
                .map(uf -> uf.getFollower().getId())
                .collect(Collectors.toList());

        List<Notification> notifications = followerIds.stream()
                .map(followerId -> Notification.builder()
                        .userId(followerId)
                        .type(NotificationType.NEW_RECORD)
                        .title("친구의 직관기록")
                        .content(recordOwner.getNickname() + "님이 직관 기록을 업로드했어요")
                        .relatedUserId(recordOwnerId)
                        .relatedRecordId(recordId)
                        .isRead(false)
                        .build())
                .collect(Collectors.toList());

        notificationRepo.saveAll(notifications);
    }

    // 5. 시스템 소식 생성
    public void createSystemNotification(String title, String content) {
        List<User> allUsers = userRepo.findAll();

        List<Notification> systemNotifications = allUsers.stream()
                .map(user -> Notification.builder()
                        .userId(user.getId())
                        .type(NotificationType.SYSTEM)
                        .title("소식")
                        .content(content)
                        .build())
                .collect(Collectors.toList());

        notificationRepo.saveAll(systemNotifications);
    }

    // 알림 목록 조회 - 카테고리 필터링으로 변경
    public List<NotificationDto> getNotifications(Long userId, String category) {
        List<Notification> notifications = notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);

        return notifications.stream()
                .map(this::convertToDto)
                .filter(dto -> "ALL".equals(category) || category.equals(dto.getCategory()))
                .collect(Collectors.toList());
    }

    // ✅ DTO 변환 메서드
    private NotificationDto convertToDto(Notification notification) {
        NotificationDto dto = NotificationDto.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .content(notification.getContent())
                .timeAgo(formatTimeAgo(notification.getCreatedAt()))
                .createdAt(notification.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .relatedRecordId(notification.getRelatedRecordId())
                .category(getCategoryFromType(notification.getType()))
                .actionButton(getActionButtonForType(notification.getType()))
                .build();

        // 사용자 정보 설정
        if (notification.getRelatedUserId() != null) {
            userRepo.findById(notification.getRelatedUserId()).ifPresent(user -> {
                dto.setUserNickname(user.getNickname());
                dto.setUserProfileImage(user.getProfileImageUrl());
            });
        } else if (notification.getType() == NotificationType.SYSTEM) {
            dto.setUserNickname("LookIT");
            dto.setUserProfileImage("/images/lookit-logo.png");
        }

        // 공감 타입 정보
        if (notification.getReactionTypeId() != null) {
            dto.setEmotionName(getEmotionName(notification.getReactionTypeId()));
            dto.setEmotionCode(notification.getReactionTypeId());
        }

        return dto;
    }

    private String getCategoryFromType(NotificationType type) {
        return switch (type) {
            case NEW_RECORD -> "FRIEND_RECORD";  // 친구가 새 게시글 올림
            case REACTION -> "REACTION";         // 내 게시글에 공감
            case FOLLOW, FOLLOW_REQUEST, SYSTEM -> "NEWS";  // 팔로우 관련 + 시스템
        };
    }

    private String getActionButtonForType(NotificationType type) {
        return switch (type) {
            case FOLLOW_REQUEST -> "ACCEPT_REJECT";  // 수락/거절 버튼
            case FOLLOW -> "FOLLOW_BUTTON";          // 맞팔/팔로잉 버튼 (상황에 따라)
            case REACTION, NEW_RECORD, SYSTEM -> null;  // 액션 버튼 없음
        };
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

        if (days < 365) {
            return createdAt.format(DateTimeFormatter.ofPattern("M월 d일"));
        } else {
            return createdAt.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"));
        }
    }
}