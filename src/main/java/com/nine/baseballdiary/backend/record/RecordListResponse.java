package com.nine.baseballdiary.backend.record;

import com.nine.baseballdiary.backend.reaction.ReactionResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class RecordListResponse {

    //사용자 정보
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String favTeam;

    //레코드 생성 시간
    private String createdAt;

    //게임 정보
    private String gameDate;
    private String gameTime;
    private String homeTeam;
    private String awayTeam;
    private Integer homeScore;
    private Integer awayScore;
    private String stadium;
    private Integer emotionCode;
    private String emotionLabel;

    //일기 내용
    private String longContent;
    private List<String> mediaUrls;

    private List<ReactionResponse> reactions;
    private Integer totalReactionCount;
    private Long recordId;

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
                              List<ReactionResponse> reactions,
                              Integer totalReactionCount) {
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
        this.reactions = reactions;
        this.totalReactionCount = totalReactionCount;
    }

}