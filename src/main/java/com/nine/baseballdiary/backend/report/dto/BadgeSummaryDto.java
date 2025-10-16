package com.nine.baseballdiary.backend.report.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class BadgeSummaryDto {
    private final int totalBadgeCount;
    private final int myBadgeCount;
    private final List<RecentBadgeDto> recentBadges; // 최근 획득한 뱃지 3개

    @Getter
    @Builder
    public static class RecentBadgeDto {
        private final String name;
        private final String imageUrl;
        private final String category;
    }
}