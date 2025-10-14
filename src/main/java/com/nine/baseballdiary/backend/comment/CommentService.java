package com.nine.baseballdiary.backend.comment;

import com.nine.baseballdiary.backend.Notifiation.NotificationService;
import com.nine.baseballdiary.backend.record.GameRecord;
import com.nine.baseballdiary.backend.record.GameRecordRepository;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final RecordCommentRepository commentRepo;
    private final UserRepository userRepo;
    private final GameRecordRepository recordRepo;
    private final NotificationService notificationService;

    // 댓글 작성
    public CommentDto createComment(Long recordId, Long userId, CommentRequest request) {
        // 게시물 존재 여부 확인
        GameRecord record = recordRepo.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));

        // 대댓글인 경우, 부모 댓글 존재 여부 확인
        if (request.getParentCommentId() != null) {
            commentRepo.findByIdNotDeleted(request.getParentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다."));
        }

        // 댓글 생성
        RecordComment comment = RecordComment.builder()
                .recordId(recordId)
                .userId(userId)
                .parentCommentId(request.getParentCommentId())
                .content(request.getContent())
                .build();

        RecordComment savedComment = commentRepo.save(comment);

        // 알림 전송
        if (request.getParentCommentId() == null) {
            // 일반 댓글: 게시물 작성자에게 알림
            if (!record.getUserId().equals(userId)) {
                notificationService.createCommentNotification(
                        record.getUserId(), userId, recordId, savedComment.getId());
            }
        } else {
            // 대댓글: 원 댓글 작성자에게 알림
            RecordComment parentComment = commentRepo.findById(request.getParentCommentId()).orElseThrow();
            if (!parentComment.getUserId().equals(userId)) {
                notificationService.createReplyNotification(
                        parentComment.getUserId(), userId, recordId, savedComment.getId());
            }
        }

        return convertToDto(savedComment, userId);
    }

    // 특정 게시물의 모든 댓글 조회
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByRecordId(Long recordId, Long currentUserId) {
        List<RecordComment> allComments = commentRepo.findAllByRecordIdNotDeleted(recordId);

        // 부모 댓글만 필터링
        List<RecordComment> parentComments = allComments.stream()
                .filter(c -> c.getParentCommentId() == null)
                .collect(Collectors.toList());

        // 각 부모 댓글에 대해 대댓글을 찾아서 DTO로 변환
        return parentComments.stream()
                .map(parent -> {
                    CommentDto dto = convertToDto(parent, currentUserId);

                    // 대댓글 찾기
                    List<CommentDto> replies = allComments.stream()
                            .filter(c -> parent.getId().equals(c.getParentCommentId()))
                            .map(reply -> convertToDto(reply, currentUserId))
                            .collect(Collectors.toList());

                    dto = CommentDto.builder()
                            .id(dto.getId())
                            .recordId(dto.getRecordId())
                            .userId(dto.getUserId())
                            .nickname(dto.getNickname())
                            .profileImageUrl(dto.getProfileImageUrl())
                            .favTeam(dto.getFavTeam())
                            .content(dto.getContent())
                            .createdAt(dto.getCreatedAt())
                            .updatedAt(dto.getUpdatedAt())
                            .isEdited(dto.isEdited())
                            .isAuthor(dto.isAuthor())
                            .replyCount((long) replies.size())
                            .replies(replies)
                            .build();

                    return dto;
                })
                .collect(Collectors.toList());
    }

    // 댓글 수정
    public CommentDto updateComment(Long commentId, Long userId, CommentRequest request) {
        RecordComment comment = commentRepo.findByIdNotDeleted(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("댓글 수정 권한이 없습니다.");
        }

        comment.updateContent(request.getContent());
        return convertToDto(comment, userId);
    }

    // 댓글 삭제 (soft delete)
    public void deleteComment(Long commentId, Long userId) {
        RecordComment comment = commentRepo.findByIdNotDeleted(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("댓글 삭제 권한이 없습니다.");
        }
        //부모 댓글인 경우 대댓글도 함께 삭제
        if (comment.getParentCommentId() == null) {
            List<RecordComment> replies = commentRepo.findRepliesByParentId(commentId);
            replies.forEach(RecordComment::delete);
        }
        comment.delete();
    }

    // 댓글 개수 조회
    @Transactional(readOnly = true)
    public long getCommentCount(Long recordId) {
        return commentRepo.countByRecordIdAndDeletedAtIsNull(recordId);
    }

    // DTO 변환 헬퍼 메서드
    private CommentDto convertToDto(RecordComment comment, Long currentUserId) {
        User user = userRepo.findById(comment.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        boolean isEdited = !comment.getCreatedAt().equals(comment.getUpdatedAt());

        return CommentDto.builder()
                .id(comment.getId())
                .recordId(comment.getRecordId())
                .userId(comment.getUserId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .favTeam(user.getFavTeam())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt().format(formatter))
                .updatedAt(comment.getUpdatedAt().format(formatter))
                .isEdited(isEdited)
                .isAuthor(comment.getUserId().equals(currentUserId))
                .replyCount(0L)  // 기본값, 필요시 조회
                .build();
    }
}