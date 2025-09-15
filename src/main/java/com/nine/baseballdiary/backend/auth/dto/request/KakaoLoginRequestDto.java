package com.nine.baseballdiary.backend.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoLoginRequestDto {
    private String code;
    private String favTeam;

}