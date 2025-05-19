package com.nine.baseballdiary.backend.record;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class RecordListResponse {
    private String gameDate;
    private String gameTime;
    private Integer emotionCode;
    private String emotionLabel;
    private String homeTeam;
    private String awayTeam;
    private String stadium;
    private String seatInfo;
    private String result;

    public RecordListResponse(String gameDate,
                              String gameTime,
                              Integer emotionCode,
                              String emotionLabel,
                              String homeTeam,
                              String awayTeam,
                              String stadium,
                              String seatInfo,
                              String result) {
        this.gameDate = gameDate;
        this.gameTime = gameTime;
        this.emotionCode = emotionCode;
        this.emotionLabel = emotionLabel;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.stadium = stadium;
        this.seatInfo = seatInfo;
        this.result = result;
    }

    // Getter, Setter 추가
}