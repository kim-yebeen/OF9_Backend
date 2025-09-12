package com.nine.baseballdiary.backend.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nine.baseballdiary.backend.search.dto.FollowStatus;
import com.nine.baseballdiary.backend.user.entity.User;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private Long id;
    private String nickname;
    private String profileImageUrl;
    private String favTeam;
    private FollowStatus followStatus;

    // ✅ private 생성자를 만들어 외부에서 직접 생성을 막고, 정적 팩토리 메서드를 통해서만 생성하도록 강제합니다.
    private UserDto(User user) {
        this.id = user.getId();
        this.nickname = user.getNickname();
        this.profileImageUrl = user.getProfileImageUrl();
        this.favTeam = user.getFavTeam();
    }

    // 기본 정보만 필요할 때 사용하는 정적 팩토리 메서드
    public static UserDto from(User user) {
        return new UserDto(user); // private 생성자 호출
    }

    // 팔로우 상태 정보까지 필요할 때 사용하는 정적 팩토리 메서드
    public static UserDto from(User user, FollowStatus status) {
        UserDto dto = new UserDto(user); // private 생성자 호출
        dto.followStatus = status; // 추가 정보 설정
        return dto;
    }
}