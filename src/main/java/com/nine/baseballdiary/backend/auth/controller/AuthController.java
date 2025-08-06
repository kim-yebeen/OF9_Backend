// src/main/java/com/nine/baseballdiary/backend/auth/AuthController.java
package com.nine.baseballdiary.backend.auth.controller;

import com.nine.baseballdiary.backend.auth.dto.request.KakaoLoginRequestDto;
import com.nine.baseballdiary.backend.auth.dto.request.RefreshTokenRequest;
import com.nine.baseballdiary.backend.auth.dto.response.AuthResponse;
import com.nine.baseballdiary.backend.auth.security.JwtProvider;
import com.nine.baseballdiary.backend.auth.service.KakaoService;
import com.nine.baseballdiary.backend.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;


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

    // AuthController.java에 추가 (기존 구조 그대로)
    // AuthController.java - 더 간단한 버전
    @PostMapping("/kakao/test")
    public ResponseEntity<AuthResponse> testLogin(@RequestBody Map<String, String> request) {
        try {
            String favTeam = request.getOrDefault("favTeam", "KIA");

            // KakaoService에 테스트용 메서드 호출
            User testUser = kakaoService.createTestUser(favTeam);

            String accessToken = jwtProvider.createAccessToken(testUser.getId().toString());
            String refreshToken = jwtProvider.createRefreshToken(testUser.getId().toString());

            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

}

