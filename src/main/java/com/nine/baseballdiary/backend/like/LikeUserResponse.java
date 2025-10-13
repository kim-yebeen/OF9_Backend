package com.nine.baseballdiary.backend.like;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@AllArgsConstructor
public class LikeUserResponse {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String favTeam;
    private String likedAt;

    public LikeUserResponse(Long userId, String nickname, String profileImageUrl, String favTeam, LocalDateTime createdAt) {
        this.userId = userId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.favTeam = favTeam;
        this.likedAt = createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}