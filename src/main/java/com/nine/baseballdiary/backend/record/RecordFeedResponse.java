package com.nine.baseballdiary.backend.record;

import lombok.Getter;
import lombok.Setter;

public class RecordFeedResponse {
    private Long recordId;
    private String gameDate;
    private String imageUrl;

    // 생성자: Long, String, String
    public RecordFeedResponse(Long recordId, String gameDate, String imageUrl) {
        this.recordId = recordId;
        this.gameDate = gameDate;
        this.imageUrl = imageUrl;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getGameDate() {
        return gameDate;
    }

    public void setGameDate(String gameDate) {
        this.gameDate = gameDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}