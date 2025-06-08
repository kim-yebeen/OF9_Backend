package com.nine.baseballdiary.backend.feed;

import com.github.dockerjava.api.exception.UnauthorizedException;
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
        if (auth != null && auth.getPrincipal() instanceof String) {
            return Long.valueOf((String) auth.getPrincipal());
        }
        throw new UnauthorizedException("인증된 사용자만 접근 가능합니다.");
    }

    // 전체 피드 조회 (내 게시물 포함)
    @GetMapping("/all")
    public ResponseEntity<List<FeedResponse>> getAllFeed(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String team,
            @RequestParam(defaultValue = "popularity") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = getCurrentUserId(); // JWT에서 추출

        FeedRequest request = FeedRequest.builder()
                .userId(userId)
                .date(date)
                .team(team)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(feedService.getAllFeed(request));
    }

    // 팔로잉 피드 조회 (팔로우한 사람들만)
    @GetMapping("/following")
    public ResponseEntity<List<FeedResponse>> getFollowingFeed(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String team,
            @RequestParam(defaultValue = "popularity") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = getCurrentUserId(); // JWT에서 추출

        FeedRequest request = FeedRequest.builder()
                .userId(userId)
                .date(date)
                .team(team)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(feedService.getFollowingFeed(request));
    }
}
