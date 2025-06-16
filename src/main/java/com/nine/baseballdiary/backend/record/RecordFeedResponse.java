package com.nine.baseballdiary.backend.record;

import java.util.List;

public class RecordFeedResponse {
    private Long recordId;
    private String gameDate;
    private List<String> mediaUrls;

    public RecordFeedResponse(Long recordId, String gameDate, List<String> mediaUrls) {
        this.recordId = recordId;
        this.gameDate = gameDate;
        // 항상 첫 번째 이미지 하나만 저장
        this.mediaUrls = (mediaUrls != null && !mediaUrls.isEmpty())
                ? List.of(mediaUrls.get(0))
                : List.of();
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

    // 항상 첫 번째 이미지 반환
    public String getImageUrl() {
        return (mediaUrls != null && !mediaUrls.isEmpty()) ? mediaUrls.get(0) : null;
    }

    // 단일 이미지만 세팅
    public void setImageUrl(String imageUrl) {
        this.mediaUrls = (imageUrl != null && !imageUrl.isEmpty())
                ? List.of(imageUrl)
                : List.of();
    }

    // 항상 첫 번째 이미지만 포함된 리스트 반환
    public List<String> getMediaUrls() {
        return mediaUrls;
    }

    // 리스트가 여러 개여도 첫 번째만 저장
    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = (mediaUrls != null && !mediaUrls.isEmpty())
                ? List.of(mediaUrls.get(0))
                : List.of();
    }
}
