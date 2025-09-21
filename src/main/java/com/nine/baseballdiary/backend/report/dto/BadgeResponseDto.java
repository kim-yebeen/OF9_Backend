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
        private final String name; // 예: "구단 도장깨기"
        private final List<BadgeDto> badges;
    }

    @Getter
    @Builder
    public static class BadgeDto {
        private final String name; // 예: "잠실 정복"
        private final String description;
        private final String imageUrl;
        private final boolean isAchieved;
        private final LocalDateTime achievedAt; // 획득한 경우에만 값이 있음
    }
}