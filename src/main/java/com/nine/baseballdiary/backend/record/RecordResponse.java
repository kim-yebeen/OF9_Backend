package com.nine.baseballdiary.backend.record;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class RecordResponse {
    private Long recordId;
    private Long userId;  // Long으로 수정
    private String gameId;
    private String seatInfo;
    private String ticketImageUrl;
    private Integer emotionCode;  // emotion_code로 수정
    private String comment;
    private String bestPlayer;
    private List<String> foodTags;
    private List<String> mediaUrls;
    private String result;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 생성자 수정
    public RecordResponse(Long recordId, Long userId, String gameId, String seatInfo, String ticketImageUrl,
                          Integer emotionCode, String comment, String bestPlayer, List<String> foodTags,
                          List<String> mediaUrls, String result, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.recordId = recordId;
        this.userId = userId;  // Long으로 수정
        this.gameId = gameId;
        this.seatInfo = seatInfo;
        this.ticketImageUrl = ticketImageUrl;
        this.emotionCode = emotionCode;  // emotion_code로 수정
        this.comment = comment;
        this.bestPlayer = bestPlayer;
        this.foodTags = foodTags;
        this.mediaUrls = mediaUrls;
        this.result = result;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
