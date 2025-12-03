package com.nine.baseballdiary.backend.feed;

import lombok.*;

@Getter
@Setter
public class FeedRequest {
    private Long userId;
    private String team;    // "LG", "두산" 등
    private String stadium; //구장
    private String seatInfo;
    private String date;

    private int page=0;
    private int size=20;
}