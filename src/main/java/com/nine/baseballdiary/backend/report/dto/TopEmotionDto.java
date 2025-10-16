package com.nine.baseballdiary.backend.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopEmotionDto {
    private final String emotion;           // "짜릿"
    private final int count;                // 사용 횟수
    private final int emotionCode;          // 감정 코드
}