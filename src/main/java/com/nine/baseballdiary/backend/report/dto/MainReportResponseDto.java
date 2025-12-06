package com.nine.baseballdiary.backend.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MainReportResponseDto {
    private final SeasonDdayDto seasonInfo;
    private final WinRateSummaryDto winRateInfo;
    private final TopEmotionDto topEmotion;              // 전체 기간 기준
    // MvpPlayerDto 삭제됨
    private final CompanionStatsDto bestCompanion;       // List → 단일 객체로 변경
    private final BadgeSummaryDto badgeSummary;
    private final TopStadiumDto topStadium;
    private final BestMonthDto bestAttendanceMonth;      // 전체 기간 기준
    private final BestMonthDto bestWinRateMonth;
}