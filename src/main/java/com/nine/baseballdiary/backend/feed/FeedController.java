package com.nine.baseballdiary.backend.feed;

import com.nine.baseballdiary.backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    // 인증 정보에서 userId 추출
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다");
        }

        if ("anonymousUser".equals(auth.getName())) {
            throw new IllegalStateException("로그인이 필요한 서비스입니다");
        }

        try {
            return Long.valueOf(auth.getName());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("올바르지 않은 토큰입니다");
        }
    }

    // 전체 피드 조회 (내 게시물 포함)
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<FeedResponse>>> getAllFeed(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String team,
            @RequestParam(defaultValue = "popularity") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Long userId = getCurrentUserId();

            FeedRequest request = FeedRequest.builder()
                    .userId(userId)
                    .date(date)
                    .team(team)
                    .sortBy(sortBy)
                    .page(page)
                    .size(size)
                    .build();

            List<FeedResponse> feedList = feedService.getAllFeed(request);

            return ResponseEntity.ok(ApiResponse.success("전체 피드를 성공적으로 조회했습니다", feedList));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("피드 조회 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }

    // 팔로잉 피드 조회 (팔로우한 사람들만)
    @GetMapping("/following")
    public ResponseEntity<ApiResponse<List<FeedResponse>>> getFollowingFeed(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String team,
            @RequestParam(defaultValue = "popularity") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Long userId = getCurrentUserId();

            FeedRequest request = FeedRequest.builder()
                    .userId(userId)
                    .date(date)
                    .team(team)
                    .sortBy(sortBy)
                    .page(page)
                    .size(size)
                    .build();

            List<FeedResponse> feedList = feedService.getFollowingFeed(request);

            return ResponseEntity.ok(ApiResponse.success("팔로잉 피드를 성공적으로 조회했습니다", feedList));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("팔로잉 피드 조회 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }
}