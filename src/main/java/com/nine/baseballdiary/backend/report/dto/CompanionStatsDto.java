package com.nine.baseballdiary.backend.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CompanionStatsDto {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private int companionCount;
    private double winRate;
}