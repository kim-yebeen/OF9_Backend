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
    public ResponseEntity<AuthResponse> login(
            @RequestHeader("Authorization") String authorization,
            @RequestBody KakaoLoginRequestDto request
    ) {
        // 1) "Bearer {token}" → "token" 으로 파싱
        String accessToken = authorization.replace("Bearer ", "").trim();

        // 2) 카카오 로그인 처리
        User user = kakaoService.processLogin(accessToken, request.getFavTeam());

        // 3) JWT 발급
        String newAccessToken  = jwtProvider.createAccessToken(user.getId().toString());
        String refreshToken = jwtProvider.createRefreshToken(user.getId().toString());

        return ResponseEntity.ok(new AuthResponse(newAccessToken, refreshToken));
    }
}

