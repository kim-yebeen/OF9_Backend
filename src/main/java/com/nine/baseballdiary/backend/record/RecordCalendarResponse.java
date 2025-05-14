package com.nine.baseballdiary.backend.record;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RecordCalendarResponse {
    private String gameDate;
    private String result;

    public RecordCalendarResponse(String gameDate, String result) {
        this.gameDate = gameDate;
        this.result = result;
    }

}