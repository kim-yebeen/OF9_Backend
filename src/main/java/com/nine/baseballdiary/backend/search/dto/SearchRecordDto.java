package com.nine.baseballdiary.backend.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRecordDto {
    private Long recordId;
    private Long authorId;
    private String authorNickname;
    private String authorProfileImage;
    private String authorFavTeam;
    private String gameDate;
    private String gameTime;
    private String homeTeam;
    private String awayTeam;
    private Integer homeScore;
    private Integer awayScore;
    private String stadium;
    private Integer emotionCode;
    private String emotionLabel;
    private String comment;
    private String longContent;
    private String result;
    private List<String> mediaUrls;
    private String createdAt;
}
