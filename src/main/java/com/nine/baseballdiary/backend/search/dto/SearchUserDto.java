package com.nine.baseballdiary.backend.search.dto;


import com.nine.baseballdiary.backend.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
public class SearchUserDto {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String favTeam;
    private Boolean isPrivate;
    private FollowStatus followStatus;
    private Boolean isMutualFollow;

    public static SearchUserDto of(User user, FollowStatus followStatus) {
        return SearchUserDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname() != null ? user.getNickname() : "알 수 없음")
                .profileImageUrl(user.getProfileImageUrl()) // 프로필 이미지는 null일 수 있음
                .favTeam(user.getFavTeam() != null ? user.getFavTeam() : "")
                .isPrivate(user.getIsPrivate())
                .followStatus(followStatus)
                .isMutualFollow(false)
                .build();
    }

    public static SearchUserDto of(User user, FollowStatus followStatus, Boolean isMutualFollow) {
        return SearchUserDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname() != null ? user.getNickname() : "알 수 없음")
                .profileImageUrl(user.getProfileImageUrl())
                .favTeam(user.getFavTeam() != null ? user.getFavTeam() : "")
                .isPrivate(user.getIsPrivate())
                .followStatus(followStatus)
                .isMutualFollow(isMutualFollow)  // ✅ 파라미터로 받은 값 사용
                .build();
    }
}