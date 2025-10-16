package com.nine.baseballdiary.backend.report.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class MainReportResponseDto {
    private final SeasonDdayDto seasonInfo;
    private final WinRateSummaryDto winRateInfo;
    private final TopEmotionDto topEmotion;           // 가장 많이 선택한 감정 1개
    private final MvpPlayerDto mvpPlayer;             // MVP 선수 1명 (List가 아님)
    private final List<CompanionStatsDto> companionStats;
    private final BadgeSummaryDto badgeSummary;
    private final TopStadiumDto topStadium;           // 최다 방문 구장 1개
    private final BestMonthDto bestAttendanceMonth;   // 직관 많이 간 달
    private final BestMonthDto bestWinRateMonth;      // 최고 승률의 달
}