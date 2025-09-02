package com.nine.baseballdiary.backend.auth.controller;

import com.nine.baseballdiary.backend.auth.dto.request.KakaoLoginRequestDto;
import com.nine.baseballdiary.backend.auth.dto.request.RefreshTokenRequest;
import com.nine.baseballdiary.backend.auth.dto.response.AuthResponse;
import com.nine.baseballdiary.backend.auth.security.JwtProvider;
import com.nine.baseballdiary.backend.auth.service.KakaoService;
import com.nine.baseballdiary.backend.common.response.ApiResponse;
import com.nine.baseballdiary.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoService kakaoService;
    private final JwtProvider jwtProvider;

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.web.redirect-uri}")
    private String kakaoWebRedirectUri;

    // === 기존 앱용 엔드포인트들 (유지) ===

    // 1. 앱용 카카오 로그인 (기존 POST 방식 유지)
    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody KakaoLoginRequestDto request) {
        try {
            System.out.println("📱 앱 카카오 로그인 요청 - favTeam: " + request.getFavTeam());

            User user = kakaoService.processLogin(request.getAccessToken(), request.getFavTeam());

            String newAccessToken = jwtProvider.createAccessToken(user.getId().toString());
            String refreshToken = jwtProvider.createRefreshToken(user.getId().toString());

            AuthResponse authResponse = new AuthResponse(newAccessToken, refreshToken);

            System.out.println("✅ 앱 로그인 성공 - userId: " + user.getId());
            return ResponseEntity.ok(ApiResponse.success(authResponse));

        } catch (Exception e) {
            System.err.println("❌ 앱 로그인 실패: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("LOGIN_ERROR", "로그인 처리 중 오류가 발생했습니다"));
        }
    }

    // 2. 토큰 갱신 (기존 유지)
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            String userId = jwtProvider.getUserIdFromToken(request.getRefreshToken());
            String newAccessToken = jwtProvider.createAccessToken(userId);

            return ResponseEntity.ok(ApiResponse.success(new AuthResponse(newAccessToken, request.getRefreshToken())));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다"));
        }
    }

    // === 웹 로그인용 엔드포인트들 (수정) ===

    // 3. 웹용 카카오 로그인 시작
    @GetMapping("/web/kakao")
    public void webKakaoLogin(
            @RequestParam(required = false, defaultValue = "KIA 타이거즈") String favTeam,
            HttpServletResponse response) throws IOException {

        System.out.println("🌐 웹 카카오 로그인 시작 - favTeam: " + favTeam);

        String kakaoAuthUrl = String.format(
                "https://kauth.kakao.com/oauth/authorize?" +
                        "client_id=%s&" +
                        "redirect_uri=%s&" +
                        "response_type=code&" +
                        "state=%s&" +
                        "scope=profile_nickname,profile_image",
                kakaoClientId,
                URLEncoder.encode(kakaoWebRedirectUri, StandardCharsets.UTF_8),
                URLEncoder.encode(favTeam, StandardCharsets.UTF_8)
        );

        System.out.println("➡️ 카카오 인증 페이지로 리다이렉트: " + kakaoAuthUrl);
        response.sendRedirect(kakaoAuthUrl);
    }

    // 4. 웹용 카카오 콜백 (핵심 수정 부분)
    @GetMapping("/web/kakao/callback")
    public void webKakaoCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletResponse response) throws IOException {

        System.out.println("⬅️ 카카오 콜백 수신");
        System.out.println("   code: " + (code != null ? code.substring(0, Math.min(10, code.length())) + "..." : "null"));
        System.out.println("   state: " + state);
        System.out.println("   error: " + error);

        if (error != null) {
            System.err.println("❌ 카카오 인증 에러: " + error);

            // 에러 시 앱으로 리다이렉트
            String errorRedirect = String.format("kakao%s://oauth?error=%s",
                    kakaoClientId, URLEncoder.encode(error, StandardCharsets.UTF_8));

            System.out.println("🔄 에러로 인한 앱 리다이렉트: " + errorRedirect);
            response.sendRedirect(errorRedirect);
            return;
        }

        try {
            String favTeam = (state != null && !state.isEmpty()) ? state : "KIA 타이거즈";
            System.out.println("🏟️ 선택된 팀: " + favTeam);

            // 카카오 웹 로그인 처리
            User user = kakaoService.processKakaoWebLogin(code, favTeam);
            System.out.println("👤 로그인된 사용자: " + user.getNickname() + " (ID: " + user.getId() + ")");

            // JWT 토큰 생성
            String accessToken = jwtProvider.createAccessToken(user.getId().toString());
            String refreshToken = jwtProvider.createRefreshToken(user.getId().toString());

            System.out.println("🔑 토큰 생성 완료");
            System.out.println("   accessToken: " + accessToken.substring(0, 20) + "...");
            System.out.println("   refreshToken: " + refreshToken.substring(0, 20) + "...");

            // 🎯 핵심: 앱으로 바로 리다이렉트 (HTML 페이지 대신)
            String successRedirect = String.format(
                    "kakao%s://oauth?access_token=%s&refresh_token=%s&user_id=%s&nickname=%s&fav_team=%s",
                    kakaoClientId,
                    URLEncoder.encode(accessToken, StandardCharsets.UTF_8),
                    URLEncoder.encode(refreshToken, StandardCharsets.UTF_8),
                    URLEncoder.encode(user.getId().toString(), StandardCharsets.UTF_8),
                    URLEncoder.encode(user.getNickname(), StandardCharsets.UTF_8),
                    URLEncoder.encode(user.getFavTeam(), StandardCharsets.UTF_8)
            );

            System.out.println("🚀 앱으로 리다이렉트: " + successRedirect);
            response.sendRedirect(successRedirect);

        } catch (Exception e) {
            System.err.println("❌ 웹 로그인 처리 오류: " + e.getMessage());
            e.printStackTrace();

            // 예외 발생 시 앱으로 에러 리다이렉트
            String errorRedirect = String.format("kakao%s://oauth?error=%s",
                    kakaoClientId, URLEncoder.encode("로그인 처리 중 오류가 발생했습니다: " + e.getMessage(), StandardCharsets.UTF_8));

            System.out.println("🔄 예외로 인한 앱 리다이렉트: " + errorRedirect);
            response.sendRedirect(errorRedirect);
        }
    }
}