package com.nine.baseballdiary.backend.like;

import com.nine.baseballdiary.backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/records/{recordId}/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("인증된 사용자가 아닙니다.");
        }
        return Long.parseLong((String) authentication.getPrincipal());
    }

    // 좋아요 토글 (추가/삭제)
    @PostMapping
    public ResponseEntity<ApiResponse<LikeResponse>> toggleLike(@PathVariable Long recordId) {
        Long userId = getCurrentUserId();
        LikeResponse response = likeService.toggleLike(userId, recordId);
        return ResponseEntity.ok(ApiResponse.success("좋아요가 처리되었습니다", response));
    }

    // 좋아요 개수 조회
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getLikeCount(@PathVariable Long recordId) {
        long count = likeService.getLikeCount(recordId);
        return ResponseEntity.ok(ApiResponse.success("좋아요 개수 조회 성공", count));
    }

    // 좋아요 여부 확인
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> checkLike(@PathVariable Long recordId) {
        Long userId = getCurrentUserId();
        boolean isLiked = likeService.isLiked(recordId, userId);
        return ResponseEntity.ok(ApiResponse.success("좋아요 여부 조회 성공", isLiked));
    }

    // 좋아요를 누른 사용자 목록 조회
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<LikeUserResponse>>> getLikeUsers(@PathVariable Long recordId) {
        List<LikeUserResponse> users = likeService.getLikeUsers(recordId);
        return ResponseEntity.ok(ApiResponse.success("좋아요를 누른 사용자 목록 조회 성공", users));
    }
}


