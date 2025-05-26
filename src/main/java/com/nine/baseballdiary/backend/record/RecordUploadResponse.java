package com.nine.baseballdiary.backend.record;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

//업로드 화면용
@Getter @Setter
public class RecordUploadResponse {
    private Long    recordId;
    private String gameDate;
    public RecordUploadResponse(Long recordId, String gameDate) {
        this.recordId = recordId;
        this.gameDate = gameDate;
    }
}