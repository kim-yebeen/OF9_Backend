package com.nine.baseballdiary.backend.search.dto;


import com.nine.baseballdiary.backend.user.entity.User;
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

    public static SearchUserDto of(User user, FollowStatus followStatus) {
        return SearchUserDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .favTeam(user.getFavTeam())
                .isPrivate(user.getIsPrivate())
                .followStatus(followStatus)
                .build();
    }
}