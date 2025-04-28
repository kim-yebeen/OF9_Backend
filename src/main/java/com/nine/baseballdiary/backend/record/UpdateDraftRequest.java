package com.nine.baseballdiary.backend.record;


import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class UpdateDraftRequest {
    private String gameDate;   // "2025-07-01"
    private String homeTeam;   // ex. "키움히어로즈"
    private String awayTeam;   // ex. "NC다이노스"
    private String startTime;  // "14:00"
    private String seatInfo;   // "1루 네이비석 309블럭 11열 4번"
}