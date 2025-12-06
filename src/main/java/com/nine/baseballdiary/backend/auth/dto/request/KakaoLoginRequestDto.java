package com.nine.baseballdiary.backend.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoLoginRequestDto {
    @NotBlank(message = "토큰은 필수입니다.") // null, "", " " 모두 허용 안 함
    private String token;
    private String favTeam;

}