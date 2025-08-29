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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletResponse;
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoService kakaoService;
    private final JwtProvider jwtProvider;

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody KakaoLoginRequestDto request) {
        try {
            User user = kakaoService.processLogin(request.getAccessToken(), request.getFavTeam());

            String accessToken = jwtProvider.createAccessToken(user.getId().toString());
            String refreshToken = jwtProvider.createRefreshToken(user.getId().toString());

            AuthResponse authResponse = new AuthResponse(accessToken, refreshToken);
            return ResponseEntity.ok(ApiResponse.success(authResponse));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("LOGIN_ERROR", "로그인 처리 중 오류가 발생했습니다"));
        }
    }

    // 토큰 갱신
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody RefreshTokenRequest request) {
        try {
            // 리프레시 토큰 검증 로직 (JwtProvider에 메서드 추가 필요)
            String userId = jwtProvider.getUserIdFromToken(request.getRefreshToken());

            String newAccessToken = jwtProvider.createAccessToken(userId);
            String newRefreshToken = jwtProvider.createRefreshToken(userId);

            AuthResponse authResponse = new AuthResponse(newAccessToken, newRefreshToken);
            return ResponseEntity.ok(ApiResponse.success(authResponse));

        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다"));
        }
    }

    // 1. 카카오 로그인 + 팀 선택 통합 URL
    @GetMapping("/kakao")
    public void kakaoLogin(
            @RequestParam(required = false, defaultValue = "KIA 타이거즈") String favTeam,
            HttpServletResponse response) throws IOException {

        String kakaoAuthUrl = String.format(
                "https://kauth.kakao.com/oauth/authorize?" +
                        "client_id=%s&" +
                        "redirect_uri=%s&" +
                        "response_type=code&" +
                        "state=%s&" +
                        "scope=profile_nickname,profile_image",
                kakaoClientId,
                URLEncoder.encode(kakaoRedirectUri, StandardCharsets.UTF_8),
                URLEncoder.encode(favTeam, StandardCharsets.UTF_8)
        );

        response.sendRedirect(kakaoAuthUrl);
    }

    // 2. 카카오 콜백 - 사용자 생성 + 토큰 반환
    @GetMapping("/kakao/callback")
    public void kakaoCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletResponse response) throws IOException {

        if (error != null) {
            response.getWriter().write(String.format(
                    "<html><body><h2>로그인 실패</h2><p>%s</p></body></html>", error));
            return;
        }

        try {
            String favTeam = (state != null && !state.isEmpty()) ? state : "KIA 타이거즈";

            // 카카오 로그인 처리 + 사용자 생성
            User user = kakaoService.processKakaoLogin(code, favTeam);

            // JWT 토큰 생성
            String accessToken = jwtProvider.createAccessToken(user.getId().toString());
            String refreshToken = jwtProvider.createRefreshToken(user.getId().toString());

            // 성공 페이지 반환 (토큰 포함)
            String successPage = createSuccessPage(user, accessToken, refreshToken);
            response.setContentType("text/html; charset=UTF-8");
            response.getWriter().write(successPage);

        } catch (Exception e) {
            String errorPage = String.format(
                    "<html><body><h2>로그인 처리 오류</h2><p>%s</p></body></html>",
                    e.getMessage()
            );
            response.getWriter().write(errorPage);
        }
    }

    // 3. 성공 페이지 HTML 생성
    private String createSuccessPage(User user, String accessToken, String refreshToken) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>로그인 완료</title>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; max-width: 600px; margin: 50px auto; padding: 20px; }
                    .success-box { background: #f0f8ff; border: 2px solid #4CAF50; padding: 20px; border-radius: 10px; }
                    .token-box { background: #f5f5f5; padding: 15px; margin: 10px 0; border-radius: 5px; word-break: break-all; }
                    button { background: #4CAF50; color: white; padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer; margin: 5px; }
                    button:hover { background: #45a049; }
                </style>
            </head>
            <body>
                <div class="success-box">
                    <h2>🎉 로그인 성공!</h2>
                    <p><strong>환영합니다, %s님!</strong></p>
                    <p>선택한 팀: <strong>%s</strong></p>
                    
                    <h3>발급된 토큰:</h3>
                    <div class="token-box">
                        <strong>Access Token:</strong><br>
                        <span id="accessToken">%s</span>
                        <button onclick="copyToken('accessToken')">복사</button>
                    </div>
                    
                    <div class="token-box">
                        <strong>Refresh Token:</strong><br>
                        <span id="refreshToken">%s</span>
                        <button onclick="copyToken('refreshToken')">복사</button>
                    </div>
                    
                    <h3>API 테스트:</h3>
                    <button onclick="testAPI()">내 정보 조회 테스트</button>
                    <div id="apiResult"></div>
                </div>
                
                <script>
                    function copyToken(elementId) {
                        const token = document.getElementById(elementId).textContent;
                        navigator.clipboard.writeText(token).then(() => {
                            alert('토큰이 클립보드에 복사되었습니다!');
                        });
                    }
                    
                    async function testAPI() {
                        const token = document.getElementById('accessToken').textContent;
                        try {
                            const response = await fetch('/users/me', {
                                headers: { 'Authorization': 'Bearer ' + token }
                            });
                            const data = await response.json();
                            document.getElementById('apiResult').innerHTML = 
                                '<h4>API 테스트 결과:</h4><pre>' + JSON.stringify(data, null, 2) + '</pre>';
                        } catch (error) {
                            document.getElementById('apiResult').innerHTML = 
                                '<h4 style="color:red">API 테스트 실패:</h4><p>' + error.message + '</p>';
                        }
                    }
                    
                    // 자동으로 토큰을 localStorage에 저장 (선택사항)
                    localStorage.setItem('access_token', '%s');
                    localStorage.setItem('refresh_token', '%s');
                </script>
            </body>
            </html>
            """,
                user.getNickname(),
                user.getFavTeam(),
                accessToken,
                refreshToken,
                accessToken,
                refreshToken
        );
    }
}