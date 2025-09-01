package com.nine.baseballdiary.backend.reaction;

import com.nine.baseballdiary.backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reactions")
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    // JWT 토큰에서 현재 사용자 ID를 가져오는 헬퍼 메서드
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다");
        }

        if ("anonymousUser".equals(authentication.getName())) {
            throw new IllegalStateException("로그인이 필요한 서비스입니다");
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("올바르지 않은 토큰입니다");
        }
    }

    // 리액션 타입 목록 조회
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<ReactionTypeResponse>>> getTypes() {
        try {
            List<ReactionTypeResponse> reactionTypes = reactionService.getAllTypes();

            return ResponseEntity.ok(ApiResponse.success("리액션 타입 목록을 성공적으로 조회했습니다", reactionTypes));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("리액션 타입 조회 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }

    // 공감 및 공감 취소 (토글)
    @PostMapping("/records/{recordId}")
    public ResponseEntity<ApiResponse<Void>> toggleReaction(
            @PathVariable Long recordId,
            @RequestBody ReactionRequest request) {
        try {
            Long userId = getCurrentUserId();
            reactionService.toggleReaction(userId, recordId, request);

            return ResponseEntity.ok(ApiResponse.success("리액션이 성공적으로 처리되었습니다"));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("리액션 처리 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }

    // 특정 게시물의 리액션 통계 조회
    @GetMapping("/records/{recordId}/stats")
    public ResponseEntity<ApiResponse<List<ReactionStatsResponse>>> getStats(
            @PathVariable Long recordId) {
        try {
            List<ReactionStatsResponse> stats = reactionService.getStats(recordId);

            return ResponseEntity.ok(ApiResponse.success("리액션 통계를 성공적으로 조회했습니다", stats));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), "NOT_FOUND"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("리액션 통계 조회 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }

    // 특정 게시물에 리액션한 사용자 목록 조회
    @GetMapping("/records/{recordId}/users")
    public ResponseEntity<ApiResponse<List<ReactionUserResponse>>> getUsers(
            @PathVariable Long recordId) {
        try {
            List<ReactionUserResponse> users = reactionService.getUsers(recordId);

            return ResponseEntity.ok(ApiResponse.success("리액션한 사용자 목록을 성공적으로 조회했습니다", users));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), "NOT_FOUND"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("리액션 사용자 조회 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }

    // 특정 게시물의 리액션 요약 정보 조회
    @GetMapping("/records/{recordId}/summary")
    public ResponseEntity<ApiResponse<RecordReactionSummary>> getSummary(
            @PathVariable Long recordId) {
        try {
            RecordReactionSummary summary = reactionService.getSummary(recordId);

            return ResponseEntity.ok(ApiResponse.success("리액션 요약 정보를 성공적으로 조회했습니다", summary));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), "NOT_FOUND"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("리액션 요약 조회 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }

    // 상위 3개 리액션 조회 (피드용)
    @GetMapping("/records/{recordId}/top")
    public ResponseEntity<ApiResponse<TopReactionsResponse>> getTopReactions(
            @PathVariable Long recordId) {
        try {
            TopReactionsResponse topReactions = reactionService.getTopReactions(recordId);

            return ResponseEntity.ok(ApiResponse.success("상위 리액션 정보를 성공적으로 조회했습니다", topReactions));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), "NOT_FOUND"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("상위 리액션 조회 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }
}