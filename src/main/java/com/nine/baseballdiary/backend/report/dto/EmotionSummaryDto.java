package com.nine.baseballdiary.backend.report.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class EmotionSummaryDto {
    private final List<EmotionCount> top3Emotions;
    private final List<EmotionRecord> allEmotions;

    public EmotionSummaryDto(List<EmotionCount> top3Emotions, List<EmotionRecord> allEmotions) {
        this.top3Emotions = top3Emotions;
        this.allEmotions = allEmotions;
    }

    // Top 3 감정 통계를 위한 내부 클래스
    @Getter
    public static class EmotionCount {
        private final String emotion; // 예: "행복"
        private final int count;
        private final int emotionCode;

        public EmotionCount(String emotion, int count, int emotionCode) {
            this.emotion = emotion;
            this.count = count;
            this.emotionCode = emotionCode;
        }
    }

    // 월간 감정 기록 (상세)을 위한 내부 클래스
    @Getter
    public static class EmotionRecord {
        private final String emotion; // 예: "짜릿해요"
        private final int count;
        private final int emotionCode;

        public EmotionRecord(String emotion, int count, int emotionCode) {
            this.emotion = emotion;
            this.count = count;
            this.emotionCode = emotionCode;
        }
    }
}