package com.nine.baseballdiary.backend.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // ✅ null 값 제외하고 JSON 응답
public class UserDto {
    private Long   id;
    private String nickname; // 필수값
    private String profileImageUrl; // null 허용
    private String favTeam; // 필수값
}