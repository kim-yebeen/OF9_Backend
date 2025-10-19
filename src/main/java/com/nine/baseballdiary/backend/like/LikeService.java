package com.nine.baseballdiary.backend.like;

import com.nine.baseballdiary.backend.Notifiation.NotificationService;
import com.nine.baseballdiary.backend.record.GameRecord;
import com.nine.baseballdiary.backend.record.GameRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class LikeService {

    private final RecordLikeRepository likeRepo;
    private final GameRecordRepository recordRepo;
    private final NotificationService notificationService;

    // 좋아요 토글 (추가/삭제)
    public LikeResponse toggleLike(Long userId, Long recordId) {
        // 게시물 존재 여부 확인
        GameRecord record = recordRepo.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));

        Optional<RecordLike> existing = likeRepo.findByRecordIdAndUserId(recordId, userId);

        if (existing.isPresent()) {
            // 이미 좋아요가 있으면 삭제
            likeRepo.delete(existing.get());

            // 좋아요 알림도 삭제 (수정)
            if (!record.getUserId().equals(userId)) {
                notificationService.deleteLikeNotification(userId, recordId);
            }

            long totalLikes = likeRepo.countByRecordId(recordId);
            return new LikeResponse(false, totalLikes);

        } else {
            // 좋아요 추가
            RecordLike newLike = RecordLike.builder()
                    .recordId(recordId)
                    .userId(userId)
                    .build();
            likeRepo.save(newLike);

            // 알림 전송 (자신의 게시물이 아닌 경우)
            if (!record.getUserId().equals(userId)) {
                notificationService.createLikeNotification(record.getUserId(), userId, recordId);
            }

            long totalLikes = likeRepo.countByRecordId(recordId);
            return new LikeResponse(true, totalLikes);
        }
    }

    // 좋아요 개수 조회
    @Transactional(readOnly = true)
    public long getLikeCount(Long recordId) {
        return likeRepo.countByRecordId(recordId);
    }

    // 좋아요 여부 확인
    @Transactional(readOnly = true)
    public boolean isLiked(Long recordId, Long userId) {
        return likeRepo.existsByRecordIdAndUserId(recordId, userId);
    }

    // 좋아요를 누른 사용자 목록 조회
    @Transactional(readOnly = true)
    public List<LikeUserResponse> getLikeUsers(Long recordId) {
        return likeRepo.findLikeUsersByRecordId(recordId);
    }
}