package com.nine.baseballdiary.backend.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BestMonthDto {
    private final int year;                 // 2025
    private final int month;                // 5
    private final int count;                // 횟수
    private final Double rate;              // 승률 (직관횟수용에는 null)
}