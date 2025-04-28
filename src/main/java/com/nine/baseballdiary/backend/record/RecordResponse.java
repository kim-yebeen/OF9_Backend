// RecordResponse.java
package com.nine.baseballdiary.backend.record;

import java.time.LocalDateTime;
import java.util.List;

public record RecordResponse(
        Integer recordId,
        String ticketImageUrl,
        String gameDate,
        String homeTeam,
        String awayTeam,
        String startTime,
        String seatInfo,
        String emotionEmoji,
        String comment,
        String bestPlayer,
        List<String> foodTags,
        List<String> mediaUrls,
        String result,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) { }
