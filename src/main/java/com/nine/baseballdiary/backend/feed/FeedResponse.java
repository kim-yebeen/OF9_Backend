package com.nine.baseballdiary.backend.feed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class FeedResponse {
    // 사용자 정보
    private Long recordId;
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String favTeam;

    // 게시물 정보
    private String createdAt;
    private String gameDate;
    private String gameTime;
    private String homeTeam;
    private String awayTeam;
    private Integer homeScore;
    private Integer awayScore;
    private String stadium;
    private Integer emotionCode;
    private String emotionLabel;
    private String longContent;
    private List<String> mediaUrls;

    // 좋아요 및 댓글 정보
    private Long likeCount;        // 좋아요 개수
    private Boolean isLiked;       // 현재 사용자가 좋아요 했는지
    private Long commentCount;     // 댓글 개수
}
