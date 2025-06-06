package com.nine.baseballdiary.backend.feed;

import com.nine.baseballdiary.backend.reaction.ReactionStatsResponse;
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

    // 리액션 정보
    private List<ReactionStatsResponse> top3Reactions;  // 상위 3개만
    private Integer remainingReactionCount;             // 나머지 개수
    private Integer totalReactionCount; // 전체 개수
}
