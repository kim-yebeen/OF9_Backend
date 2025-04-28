// UploadRecordResponse.java
package com.nine.baseballdiary.backend.record;

public record UploadRecordResponse(
        Integer recordId,
        String ticketImageUrl,
        String gameDate,
        String homeTeam,
        String awayTeam,
        String startTime,
        String seatInfo
) { }
