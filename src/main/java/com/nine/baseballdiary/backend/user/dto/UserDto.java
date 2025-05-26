package com.nine.baseballdiary.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class UserDto {
    private Long   id;
    private String nickname;
    private String profileImageUrl;
    private String favTeam; //추가!
}