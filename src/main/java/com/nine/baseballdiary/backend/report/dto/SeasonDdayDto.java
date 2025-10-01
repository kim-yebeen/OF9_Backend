package com.nine.baseballdiary.backend.report.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SeasonDdayDto {
    private int seasonYear;
    private int daysRemaining;
    private String seasonEndDate;
    private String status; // IN_PROGRESS, BEFORE_START, ENDED
    private String message; // "2025 시즌 종료까지", "2026 시즌 시작까지"
}
