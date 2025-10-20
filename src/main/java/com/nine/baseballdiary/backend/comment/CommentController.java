package com.nine.baseballdiary.backend.comment;

import com.nine.baseballdiary.backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/records/{recordId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("인증된 사용자가 아닙니다.");
        }
        return Long.parseLong((String) authentication.getPrincipal());
    }

    // 댓글 작성
    @PostMapping
    public ResponseEntity<ApiResponse<CommentDto>> createComment(
            @PathVariable Long recordId,
            @RequestBody CommentRequest request) {
        Long userId = getCurrentUserId();
        CommentDto comment = commentService.createComment(recordId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("댓글이 작성되었습니다", comment));
    }

    // 특정 게시물의 모든 댓글 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentDto>>> getComments(@PathVariable Long recordId) {
        Long userId = getCurrentUserId();
        List<CommentDto> comments = commentService.getCommentsByRecordId(recordId, userId);
        return ResponseEntity.ok(ApiResponse.success("댓글 목록 조회 성공", comments));
    }

    // 댓글 수정
    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentDto>> updateComment(
            @PathVariable Long recordId,
            @PathVariable Long commentId,
            @RequestBody CommentRequest request) {
        Long userId = getCurrentUserId();
        CommentDto comment = commentService.updateComment(commentId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("댓글이 수정되었습니다", comment));
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentDto>> deleteComment(@PathVariable Long commentId) {
        Long userId = getCurrentUserId();
        CommentDto result = commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success("댓글이 삭제되었습니다", result));
    }

        // 댓글 개수 조회
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCommentCount(@PathVariable Long recordId) {
        long count = commentService.getCommentCount(recordId);
        return ResponseEntity.ok(ApiResponse.success("댓글 개수 조회 성공", count));
    }
}
