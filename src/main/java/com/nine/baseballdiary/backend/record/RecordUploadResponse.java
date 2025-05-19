package com.nine.baseballdiary.backend.record;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

//업로드 화면용
@Getter@AllArgsConstructor@Setter
public class RecordUploadResponse {
    private Long    recordId;
    private String gameDate;
    private String gameTime;
    private Integer emotionCode;
    private String emotionLabel;
    private String homeTeam;
    private String awayTeam;
    private String stadium;
    private String seatInfo;
    private Integer homeScore;
    private Integer awayScore;
    private String result;  // WIN/LOSE/DRAW 결과 추가

}