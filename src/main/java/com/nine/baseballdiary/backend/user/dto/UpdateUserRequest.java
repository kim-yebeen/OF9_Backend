package com.nine.baseballdiary.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 1, max = 15, message = "닉네임은 1자 이상 15자 이하여야 합니다")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9\\s_-]+$", message = "닉네임은 한글, 영문, 숫자, 공백, _, - 만 사용 가능합니다")
    private String nickname;

    @Size(max = 500, message = "프로필 이미지 URL은 500자를 초과할 수 없습니다")
    private String profileImageUrl;

    @Size(max = 20, message = "팬팀 이름은 20자를 초과할 수 없습니다")
    private String favTeam;

    private Boolean isPrivate;
}