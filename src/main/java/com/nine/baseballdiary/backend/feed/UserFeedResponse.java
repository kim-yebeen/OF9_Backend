package com.nine.baseballdiary.backend.feed;

import com.nine.baseballdiary.backend.search.dto.FollowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class UserFeedResponse {
    // 사용자 정보
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String favTeam;
    private Boolean isPrivate;
    private FollowStatus followStatus;
    private Boolean isMutualFollow;

    // 통계
    private Long recordCount;
    private Long followerCount;
    private Long followingCount;

    // 피드 아이템들
    private List<UserFeedItem> feedItems;
}