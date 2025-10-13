package com.nine.baseballdiary.backend.like;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LikeResponse {
    private boolean isLiked;
    private long totalLikes;
}