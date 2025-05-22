package com.nine.baseballdiary.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class UserProfileDto {
    private Long   id;
    private String nickname;
    private String profileImageUrl;
    private String favTeam;
    private Boolean isPrivate;
    private Long   followerCount;
    private Long   followingCount;
    private Long   recordCount;
}