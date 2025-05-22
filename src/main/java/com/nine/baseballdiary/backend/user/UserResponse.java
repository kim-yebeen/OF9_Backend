package com.nine.baseballdiary.backend.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {
    private String nickname;
    private String favTeam;      // 팬팀 (이름)
    private String profileImageUrl;
    private boolean isPrivate;
    private long postCount;      // 게시글 수
    private long followingCount; // 팔로잉 수
    private long followerCount;  // 팔로워 수
}
