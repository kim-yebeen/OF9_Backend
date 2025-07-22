// src/main/java/com/nine/baseballdiary/backend/auth/AuthController.java
package com.nine.baseballdiary.backend.auth;

import com.nine.baseballdiary.backend.user.entity.User;
import org.springframework.http.HttpStatus;
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
        // ✅ @RequestHeader 제거, @RequestBody에서 accessToken 받기
        User user = kakaoService.processLogin(request.getAccessToken(), request.getFavTeam());

        String newAccessToken = jwtProvider.createAccessToken(user.getId().toString());
        String refreshToken = jwtProvider.createRefreshToken(user.getId().toString());

        return ResponseEntity.ok(new AuthResponse(newAccessToken, refreshToken));
    }

    //토큰 갱신 엔드포인트
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            // 1) Refresh Token 검증
            String userId = jwtProvider.getUserIdFromToken(request.getRefreshToken());

            // 2) 새로운 Access Token 발급 (Refresh Token은 그대로 유지)
            String newAccessToken = jwtProvider.createAccessToken(userId);

            return ResponseEntity.ok(new AuthResponse(newAccessToken, request.getRefreshToken()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}

