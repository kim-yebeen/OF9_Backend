package com.nine.baseballdiary.backend.report.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class WinRateSummaryDto {
    private final double totalWinRate;
    private final int totalWinCount;
    private final int totalLoseCount;
    private final int totalDrawCount;
    private final int totalGameCount;

    // 홈/원정 승률 추가
    private final double homeWinRate;
    private final int homeWinCount;
    private final int homeLoseCount;
    private final int homeGameCount;

    private final double awayWinRate;
    private final int awayWinCount;
    private final int awayLoseCount;
    private final int awayGameCount;

    private final List<TeamWinRate> teamWinRates;

    @Getter
    @Builder
    public static class TeamWinRate {
        private final String teamName;
        private final double winRate;
        private final int recordCount; // 해당 팀을 직관한 횟수
    }
}