package com.nine.baseballdiary.backend.record;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class RecordListResponse {

    // 사용자 정보
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String favTeam;

    // 레코드 생성 시간
    private String createdAt;

    // 게임 정보
    private String gameDate;
    private String gameTime;
    private String homeTeam;
    private String awayTeam;
    private Integer homeScore;
    private Integer awayScore;
    private String stadium;
    private Integer emotionCode;
    private String emotionLabel;

    // 일기 내용
    private String longContent;
    private List<String> mediaUrls;

    // 좋아요 및 댓글 정보로 변경
    private Long likeCount;        // 좋아요 개수
    private Boolean isLiked;       // 현재 사용자가 좋아요 했는지
    private Long commentCount;     // 댓글 개수

    // recordId 추가
    private Long recordId;

    // 생성자 수정
    public RecordListResponse(Long userId,
                              String nickname,
                              String profileImageUrl,
                              String favTeam,
                              String createdAt,
                              String gameDate,
                              String gameTime,
                              String homeTeam,
                              String awayTeam,
                              Integer homeScore,
                              Integer awayScore,
                              String stadium,
                              Integer emotionCode,
                              String emotionLabel,
                              String longContent,
                              List<String> mediaUrls,
                              Long likeCount,
                              Boolean isLiked,
                              Long commentCount,
                              Long recordId
    ) {
        this.userId = userId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.favTeam = favTeam;
        this.createdAt = createdAt;
        this.gameDate = gameDate;
        this.gameTime = gameTime;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.stadium = stadium;
        this.emotionCode = emotionCode;
        this.emotionLabel = emotionLabel;
        this.longContent = longContent;
        this.mediaUrls = mediaUrls;
        this.likeCount = likeCount;
        this.isLiked = isLiked;
        this.commentCount = commentCount;
        this.recordId = recordId;
    }
}
