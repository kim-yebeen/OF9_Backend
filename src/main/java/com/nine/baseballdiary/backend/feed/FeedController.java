package com.nine.baseballdiary.backend.feed;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    // 전체 피드 조회 (내 게시물 포함)
    @GetMapping("/all")
    public ResponseEntity<List<FeedResponse>> getAllFeed(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String team,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        FeedRequest request = FeedRequest.builder()
                .userId(userId)
                .date(date)
                .team(team)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(feedService.getAllFeed(request));
    }

    // 팔로잉 피드 조회 (팔로우한 사람들만)
    @GetMapping("/following")
    public ResponseEntity<List<FeedResponse>> getFollowingFeed(
            @RequestParam Long userId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String team,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        FeedRequest request = FeedRequest.builder()
                .userId(userId)
                .date(date)
                .team(team)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(feedService.getFollowingFeed(request));
    }
}
