package com.nine.baseballdiary.backend.search.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchUserDto {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String favTeam;
    private Boolean isPrivate;
    private FollowStatus followStatus;
}