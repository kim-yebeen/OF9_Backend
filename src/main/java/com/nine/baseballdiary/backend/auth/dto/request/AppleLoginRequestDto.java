package com.nine.baseballdiary.backend.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AppleLoginRequestDto {
    private String identityToken; // 프론트에서 받은 핵심 토큰
    private String user;          // (옵션) JSON 문자열, 이름 정보가 들어있음
    private String favTeam;       // 회원가입 시 받을 응원팀
}
