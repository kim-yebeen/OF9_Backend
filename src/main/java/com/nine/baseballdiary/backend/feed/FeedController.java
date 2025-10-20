package com.nine.baseballdiary.backend.feed;

import com.nine.baseballdiary.backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class FeedController {
    private final FeedService feedService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("인증된 사용자가 아닙니다.");
        }
        return Long.parseLong((String) authentication.getPrincipal());
    }

    /**
     * 전체 피드 조회 (최신순)
     * @param team 팀 필터 (선택, 예: "LG", "두산")
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<FeedResponse>>> getAllFeed(
            @RequestParam(required = false) String team,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = getCurrentUserId();

        FeedRequest request = new FeedRequest();
        request.setUserId(userId);
        request.setTeam(team);
        request.setPage(page);
        request.setSize(size);

        List<FeedResponse> response = feedService.getAllFeed(request);
        return ResponseEntity.ok(ApiResponse.success("전체 피드를 성공적으로 조회했습니다", response));
    }

    /**
     * 팔로잉 피드 조회 (최신순)
     * @param team 팀 필터 (선택, 예: "LG", "두산")
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     */
    @GetMapping("/following")
    public ResponseEntity<ApiResponse<List<FeedResponse>>> getFollowingFeed(
            @RequestParam(required = false) String team,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = getCurrentUserId();

        FeedRequest request = new FeedRequest();
        request.setUserId(userId);
        request.setTeam(team);
        request.setPage(page);
        request.setSize(size);

        List<FeedResponse> response = feedService.getFollowingFeed(request);
        return ResponseEntity.ok(ApiResponse.success("팔로잉 피드를 성공적으로 조회했습니다", response));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<UserFeedResponse>> getUserFeed(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        UserFeedResponse response = feedService.getUserFeed(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("사용자 피드를 성공적으로 조회했습니다", response));
    }
}