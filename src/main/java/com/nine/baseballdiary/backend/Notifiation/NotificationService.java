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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final UserFollowRepository userFollowRepo;
   // private final GameRecordRepository recordRepo;
    private final FcmService fcmService;

    // 1. 좋아요 알림 생성
    public void createLikeNotification(Long recordOwnerId, Long likerId, Long recordId) {
        if (recordOwnerId.equals(likerId)) return;

        User liker = userRepo.findById(likerId).orElseThrow();
        User recordOwner = userRepo.findById(recordOwnerId).orElseThrow();

        // ✅ DB에 알림은 항상 저장
        Notification notification = Notification.builder()
                .userId(recordOwnerId)
                .type(NotificationType.LIKE)
                .title("받은 공감")
                .content("님이 나의 직관기록에 좋아요를 남겼어요.")
                .relatedUserId(likerId)
                .relatedRecordId(recordId)
                .isRead(false)
                .build();

        notificationRepo.save(notification);

        // ✅ 푸시 알림은 설정이 켜져있고 FCM 토큰이 있을 때만 발송
        if (Boolean.TRUE.equals(recordOwner.getPushEnabled())
                && recordOwner.getFcmToken() != null
                && !recordOwner.getFcmToken().isEmpty()) {
            fcmService.sendNotification(
                    recordOwner.getFcmToken(),
                    "받은 공감",
                    liker.getNickname() + "님이 나의 직관기록에 좋아요를 남겼어요.",
                    "LIKE",
                    String.valueOf(recordId)
            );
        }
    }

    // 2. 댓글 알림 생성
    public void createCommentNotification(Long recordOwnerId, Long commenterId, Long recordId, Long commentId) {
        if (recordOwnerId.equals(commenterId)) return;

        User commenter = userRepo.findById(commenterId).orElseThrow();
        User recordOwner = userRepo.findById(recordOwnerId).orElseThrow();

        Notification notification = Notification.builder()
                .userId(recordOwnerId)
                .type(NotificationType.COMMENT)
                .title("댓글")
                .content("님이 나의 직관기록에 댓글을 남겼어요.")
                .relatedUserId(commenterId)
                .relatedRecordId(recordId)
                .relatedCommentId(commentId)
                .isRead(false)
                .build();

        notificationRepo.save(notification);

        // ✅ 푸시 설정 확인
        if (Boolean.TRUE.equals(recordOwner.getPushEnabled())
                && recordOwner.getFcmToken() != null
                && !recordOwner.getFcmToken().isEmpty()) {
            fcmService.sendNotification(
                    recordOwner.getFcmToken(),
                    "댓글",
                    commenter.getNickname() + "님이 나의 직관기록에 댓글을 남겼어요.",
                    "COMMENT",
                    String.valueOf(recordId)
            );
        }
    }

    // 3. 답글 알림 생성
    public void createReplyNotification(Long parentCommentOwnerId, Long replierId, Long recordId, Long replyId) {
        if (parentCommentOwnerId.equals(replierId)) return;

        User replier = userRepo.findById(replierId).orElseThrow();
        User parentOwner = userRepo.findById(parentCommentOwnerId).orElseThrow();

        Notification notification = Notification.builder()
                .userId(parentCommentOwnerId)
                .type(NotificationType.REPLY)
                .title("답글")
                .content("님이 나의 댓글에 답글을 남겼어요.")
                .relatedUserId(replierId)
                .relatedRecordId(recordId)
                .relatedCommentId(replyId)
                .isRead(false)
                .build();

        notificationRepo.save(notification);

        // ✅ 푸시 설정 확인
        if (Boolean.TRUE.equals(parentOwner.getPushEnabled())
                && parentOwner.getFcmToken() != null
                && !parentOwner.getFcmToken().isEmpty()) {
            fcmService.sendNotification(
                    parentOwner.getFcmToken(),
                    "답글",
                    replier.getNickname() + "님이 나의 댓글에 답글을 남겼어요.",
                    "REPLY",
                    String.valueOf(recordId)
            );
        }
    }

    // 4. 팔로우 알림
    public void createFollowNotification(Long followeeId, Long followerId) {
        User follower = userRepo.findById(followerId).orElseThrow();
        User followee = userRepo.findById(followeeId).orElseThrow();

        Notification notification = Notification.builder()
                .userId(followeeId)
                .type(NotificationType.FOLLOW)
                .title("팔로우")
                .content("님이 나를 팔로우 했어요.")
                .relatedUserId(followerId)
                .isRead(false)
                .build();

        notificationRepo.save(notification);

        // ✅ 푸시 설정 확인
        if (Boolean.TRUE.equals(followee.getPushEnabled())
                && followee.getFcmToken() != null
                && !followee.getFcmToken().isEmpty()) {
            fcmService.sendNotification(
                    followee.getFcmToken(),
                    "팔로우",
                    follower.getNickname() + "님이 나를 팔로우 했어요.",
                    "FOLLOW",
                    String.valueOf(followerId)
            );
        }
    }

    // 5. 팔로우 요청 알림
    public void createFollowRequestNotification(Long targetId, Long requesterId) {
        User requester = userRepo.findById(requesterId).orElseThrow();
        User target = userRepo.findById(targetId).orElseThrow();

        Notification notification = Notification.builder()
                .userId(targetId)
                .type(NotificationType.FOLLOW_REQUEST)
                .title("팔로우 요청")
                .content("님이 팔로우를 요청했어요.")
                .relatedUserId(requesterId)
                .isRead(false)
                .build();

        notificationRepo.save(notification);

        // ✅ 푸시 설정 확인
        if (Boolean.TRUE.equals(target.getPushEnabled())
                && target.getFcmToken() != null
                && !target.getFcmToken().isEmpty()) {
            fcmService.sendNotification(
                    target.getFcmToken(),
                    "팔로우 요청",
                    requester.getNickname() + "님이 팔로우를 요청했어요.",
                    "FOLLOW_REQUEST",
                    String.valueOf(requesterId)
            );
        }
    }

    public void createNewRecordNotification(Long recordOwnerId, Long recordId) {
        User recordOwner = userRepo.findById(recordOwnerId).orElseThrow();

        List<User> followers = userFollowRepo.findByFollowee_Id(recordOwnerId)
                .stream()
                .map(uf -> uf.getFollower())
                .collect(Collectors.toList());

        List<Notification> notifications = followers.stream()
                .map(follower -> Notification.builder()
                        .userId(follower.getId())
                        .type(NotificationType.NEW_RECORD)
                        .title("친구의 직관기록")
                        .content("님이 직관 기록을 업로드했어요.")
                        .relatedUserId(recordOwnerId)
                        .relatedRecordId(recordId)
                        .isRead(false)
                        .build())
                .collect(Collectors.toList());

        notificationRepo.saveAll(notifications);

        // ✅ 푸시 설정 확인 후 발송
        for (User follower : followers) {
            if (Boolean.TRUE.equals(follower.getPushEnabled())
                    && follower.getFcmToken() != null
                    && !follower.getFcmToken().isEmpty()) {
                fcmService.sendNotification(
                        follower.getFcmToken(),
                        "친구의 직관기록",
                        recordOwner.getNickname() + "님이 직관 기록을 업로드했어요.",
                        "NEW_RECORD",
                        String.valueOf(recordId)
                );
            }
        }
    }
    // 7. 시스템 소식 생성
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

    // DTO 변환 메서드
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

            if (notification.getType() == NotificationType.FOLLOW ||
                    notification.getType() == NotificationType.FOLLOW_REQUEST) {

                Long myId = notification.getUserId();
                Long otherId = notification.getRelatedUserId();

                dto.setIsFollowing(userFollowRepo.existsByFollower_IdAndFollowee_Id(myId, otherId));
                dto.setIsFollower(userFollowRepo.existsByFollower_IdAndFollowee_Id(otherId, myId));
            }
        } else if (notification.getType() == NotificationType.SYSTEM) {
            dto.setUserNickname("LookIT");
            dto.setUserProfileImage("/images/lookit-logo.png");
        }

        return dto;
    }

    private String getCategoryFromType(NotificationType type) {
        return switch (type) {
            case NEW_RECORD -> "FRIEND_RECORD";
            case LIKE, COMMENT, REPLY -> "REACTION";
            case FOLLOW, FOLLOW_REQUEST, SYSTEM -> "NEWS";
        };
    }

    private String getActionButtonForType(NotificationType type) {
        return switch (type) {
            case FOLLOW_REQUEST -> "ACCEPT_REJECT";
            case FOLLOW -> "FOLLOW_BUTTON";
            case LIKE, COMMENT, REPLY, NEW_RECORD, SYSTEM -> null;
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

    public void deleteLikeNotification(Long likerId, Long recordId) {
        notificationRepo.deleteByTypeAndRelatedUserIdAndRelatedRecordId(
                NotificationType.LIKE, likerId, recordId);
    }

    public void deleteCommentNotification(Long commenterId, Long commentId) {
        notificationRepo.deleteByTypeAndRelatedUserIdAndRelatedCommentId(
                NotificationType.COMMENT, commenterId, commentId);
    }
}
