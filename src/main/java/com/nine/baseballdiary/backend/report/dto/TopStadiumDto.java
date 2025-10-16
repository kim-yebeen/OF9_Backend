package com.nine.baseballdiary.backend.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopStadiumDto {
    private final String stadiumName;       // "잠실"
    private final int visitCount;           // 방문 횟수
    private final double winRate;           // 해당 구장 승률
    private final String city;              // "서울"
}