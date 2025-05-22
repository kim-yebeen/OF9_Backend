// src/main/java/com/nine/baseballdiary/backend/auth/AuthController.java
package com.nine.baseballdiary.backend.auth;

import com.nine.baseballdiary.backend.user.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final KakaoService kakaoService;
    private final JwtProvider jwtProvider;

    public AuthController(KakaoService kakaoService, JwtProvider jwtProvider) {
        this.kakaoService = kakaoService;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/kakao")
    public ResponseEntity<AuthResponse> login(@RequestBody KakaoLoginRequestDto request) {
        // 1) 카카오 엑세스 토큰으로 사용자 처리 (회원가입 혹은 조회)
        User user = kakaoService.processLogin(request.getAccessToken(), request.getFavTeam());

        // 2) AccessToken, RefreshToken 발급
        String accessToken  = jwtProvider.createAccessToken(user.getId().toString());
        String refreshToken = jwtProvider.createRefreshToken(user.getId().toString());

        // 3) 두 토큰을 담아 응답
        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
    }
}

