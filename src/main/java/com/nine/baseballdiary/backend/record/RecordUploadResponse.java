package com.nine.baseballdiary.backend.record;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

//업로드 화면용
@Getter @Setter
public class RecordUploadResponse {
    private Long    recordId;
    private String gameDate;
    private boolean isFirstRecord;

    public RecordUploadResponse(Long recordId, String gameDate, boolean isFirstRecord) {
        this.recordId = recordId;
        this.gameDate = gameDate;
        this.isFirstRecord = isFirstRecord;
    }
}