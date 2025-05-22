package com.nine.baseballdiary.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    @NotBlank String nickname;
    String profileImageUrl;
    String favTeam;
    Boolean isPrivate;
}