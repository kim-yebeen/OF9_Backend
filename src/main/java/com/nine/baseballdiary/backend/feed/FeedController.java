package com.nine.baseballdiary.backend.feed;

import com.nine.baseballdiary.backend.common.response.ApiResponse;
import com.nine.baseballdiary.backend.record.RecordListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

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

    // 1. 리스트 뷰 API 추가
    @GetMapping("/user/{userId}/list")
    public ResponseEntity<ApiResponse<List<RecordListResponse>>> getUserList(
            @PathVariable Long userId) {
        try {
            Long currentUserId = getCurrentUserId();
            List<RecordListResponse> response = feedService.getUserList(currentUserId, userId);
            return ResponseEntity.ok(ApiResponse.success("사용자 리스트를 성공적으로 조회했습니다", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), "NOT_FOUND"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("조회 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }

    // 2. 캘린더 뷰 API 추가
    @GetMapping("/user/{userId}/calendar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserCalendar(
            @PathVariable Long userId,
            @RequestParam int year,
            @RequestParam int month) {
        try {
            Long currentUserId = getCurrentUserId();
            Map<String, Object> response = feedService.getUserCalendar(currentUserId, userId, year, month);
            return ResponseEntity.ok(ApiResponse.success("사용자 캘린더를 성공적으로 조회했습니다", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), "NOT_FOUND"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("조회 중 서버 오류가 발생했습니다", "INTERNAL_SERVER_ERROR"));
        }
    }
}