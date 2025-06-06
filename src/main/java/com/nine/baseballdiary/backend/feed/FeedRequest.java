package com.nine.baseballdiary.backend.feed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeedRequest {
    private Long userId;
    private String date;    // "2025-06-05" 형식
    private String team;    // "LG", "두산" 등
    private int page;
    private int size;
}