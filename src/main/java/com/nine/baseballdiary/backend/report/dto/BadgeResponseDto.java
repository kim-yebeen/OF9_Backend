package com.nine.baseballdiary.backend.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BadgeResponseDto {
    private final int totalBadgeCount;
    private final int myBadgeCount;
    private final List<BadgeCategoryDto> categories;

    @Getter
    @Builder
    public static class BadgeCategoryDto {
        private final String name; // 예: "어서와, 야구 직관은 처음이지?"
        private final List<BadgeDto> badges;
    }


    @Getter
    @Builder
    public static class BadgeDto {
        private final String name; // 예: "기록의 시작"
        private final String description;
        private final String imageUrl;
        private final boolean isAchieved;
        private final LocalDateTime achievedAt; // ✅ 추가: 획득한 경우에만 값이 있음
    }
}