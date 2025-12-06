package com.nine.baseballdiary.backend.report.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class BadgeSummaryDto {
    private final int totalBadgeCount;
    private final int myBadgeCount;
    private final List<MainPageBadgeDto> mainPageBadges; // 메인 페이지 5개 슬롯 중 획득한 것만

    @Getter
    @Builder
    public static class MainPageBadgeDto {
        private final Integer badgeId;        // badge 테이블의 PK
        private final String badgeName;       // 예: "기록의 시작", "사자 정복"
        private final String imageUrl;        // 뱃지 이미지 URL
        private final int slotOrder;          // 1~5 (왼쪽부터)
    }
}