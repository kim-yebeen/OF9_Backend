package com.nine.baseballdiary.backend.record;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RecordCalendarResponse {
    private String gameDate;
    private String result;

    private Double winRate;      // 직관 승률 (%)
    private Integer recordCount; // 기록 횟수
    private Long totalLikes;     // 공감받은 횟수 (좋아요 총합)

    public RecordCalendarResponse(String gameDate, String result) {
        this.gameDate = gameDate;
        this.result = result;
        this.winRate = null;
        this.recordCount = null;
        this.totalLikes = null;
    }

    // 새로운 생성자 (통계 포함)
    public RecordCalendarResponse(String gameDate, String result, Double winRate, Integer recordCount, Long totalLikes) {
        this.gameDate = gameDate;
        this.result = result;
        this.winRate = winRate;
        this.recordCount = recordCount;
        this.totalLikes = totalLikes;
    }
}