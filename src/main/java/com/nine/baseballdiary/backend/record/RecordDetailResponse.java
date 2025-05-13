package com.nine.baseballdiary.backend.record;

import lombok.Getter;

@Getter
public class RecordDetailResponse {
    private String gameDate;
    private String gameTime;
    private Integer emotionCode;
    private String emotionLabel;
    private String ticketImageUrl;
    private String homeTeam;
    private String awayTeam;
    private String stadium;
    private String seatInfo;

    // 생성자
    public RecordDetailResponse(String gameDate, String gameTime, Integer emotionCode,
                                String emotionLabel, String ticketImageUrl, String homeTeam,
                                String awayTeam, String stadium, String seatInfo) {
        this.gameDate = gameDate;
        this.gameTime = gameTime;
        this.emotionCode = emotionCode;
        this.emotionLabel = emotionLabel;
        this.ticketImageUrl = ticketImageUrl;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.stadium = stadium;
        this.seatInfo = seatInfo;
    }

    // Getters and Setters
}
