package com.nine.baseballdiary.backend.record;

import com.nine.baseballdiary.backend.reaction.ReactionStatsResponse;
import com.nine.baseballdiary.backend.user.dto.UserDto;
import lombok.Getter;
import lombok.Builder;
import lombok.Setter;

import java.util.List;
//클릭 시 상세 보기 용

@Getter @Setter
@Builder
public class RecordDetailResponse {
    private Long recordId;
    private String gameDate;
    private String gameTime;
    private Integer emotionCode;
    private String emotionLabel;
    private String ticketImageUrl;
    private String homeTeam;
    private String awayTeam;
    private String stadium;
    private String seatInfo;
    private Integer homeScore;
    private Integer awayScore;
    private String result;
    private String comment;
    private String longContent;
    private String bestPlayer;
    private List<UserDto> companions;
    private List<String> foodTags;
    private List<String> mediaUrls;
    private String createdAt;
    // 리액션 정보 추가
    private List<ReactionStatsResponse> reactions;
    private Integer totalReactionCount;

}