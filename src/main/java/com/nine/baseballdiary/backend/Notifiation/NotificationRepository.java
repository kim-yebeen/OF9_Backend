package com.nine.baseballdiary.backend.Notifiation;

import com.nine.baseballdiary.backend.Notifiation.Notification;
import com.nine.baseballdiary.backend.Notifiation.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type);

    Long countByUserIdAndIsReadFalse(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId")
    void updateAllAsReadByUserId(@Param("userId") Long userId);
    // 좋아요 알림 삭제 (relatedUserId = 좋아요한 사람, relatedRecordId = 게시글)
    void deleteByTypeAndRelatedUserIdAndRelatedRecordId(NotificationType type, Long relatedUserId, Long relatedRecordId);

    // 댓글 알림 삭제 (relatedUserId = 댓글 작성자, relatedCommentId = 댓글)
    void deleteByTypeAndRelatedUserIdAndRelatedCommentId(NotificationType type, Long relatedUserId, Long relatedCommentId);
}