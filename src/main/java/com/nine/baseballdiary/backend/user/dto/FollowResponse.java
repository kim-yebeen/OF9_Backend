package com.nine.baseballdiary.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowResponse {
    private boolean followed;   // 실제 팔로우 관계 생성됨
    private boolean pending;    // 비공개 계정인 경우 요청만 남음
    private Long    requestId;  // PENDING 요청 ID (없으면 null)

    private Boolean isFollowing;   // 내가 상대방을 팔로우하고 있는지
    private Boolean isFollower;    // 상대방이 나를 팔로우하고 있는지
    private Boolean isMutual;      // 맞팔 관계인지
}

