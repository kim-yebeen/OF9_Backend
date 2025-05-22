package com.nine.baseballdiary.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FollowResponse {
    private boolean followed;   // 실제 팔로우 관계 생성됨
    private boolean pending;    // 비공개 계정인 경우 요청만 남음
    private Long    requestId;  // PENDING 요청 ID (없으면 null)
}

