package com.nine.baseballdiary.backend.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserRankingDto {
    private final int rank;
    private final String nickname;
    private final String profileImageUrl;
    private final String favTeam;
    private final long score;
}