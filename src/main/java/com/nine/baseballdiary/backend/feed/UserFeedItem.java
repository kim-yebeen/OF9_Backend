package com.nine.baseballdiary.backend.feed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserFeedItem {
    private Long recordId;
    private String gameDate;
    private String imageUrl;
    private Long likeCount;
}