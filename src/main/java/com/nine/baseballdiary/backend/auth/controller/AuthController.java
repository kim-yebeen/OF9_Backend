package com.nine.baseballdiary.backend.auth.controller;

import com.nine.baseballdiary.backend.auth.dto.request.KakaoCheckRequestDto;
import com.nine.baseballdiary.backend.auth.dto.request.KakaoLoginRequestDto;
import com.nine.baseballdiary.backend.auth.dto.request.RefreshTokenRequest;
import com.nine.baseballdiary.backend.auth.dto.response.AuthResponse;
import com.nine.baseballdiary.backend.auth.security.JwtProvider;
import com.nine.baseballdiary.backend.auth.service.KakaoService;
import com.nine.baseballdiary.backend.common.response.ApiResponse;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoService kakaoService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.web.redirect-uri}")
    private String kakaoWebRedirectUri;

    // === 기존 앱용 엔드포인트들 ===

    // 1. 앱용 카카오 로그인 (기존 POST 방식 유지)
    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody KakaoLoginRequestDto request) {
        try {
            User user = kakaoService.processLogin(request.getAccessToken(), request.getFavTeam());

            String newAccessToken = jwtProvider.createAccessToken(user.getId().toString());
            String refreshToken = jwtProvider.createRefreshToken(user.getId().toString());

            AuthResponse authResponse = new AuthResponse(newAccessToken, refreshToken);
            return ResponseEntity.ok(ApiResponse.success(authResponse));

        } catch (Exception e) {
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

    // === 새로 추가되는 웹용 엔드포인트들 ===

    // 3. 웹용 카카오 로그인 시작
    @GetMapping("/web/kakao")
    public void webKakaoLogin(
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
                URLEncoder.encode(kakaoWebRedirectUri, StandardCharsets.UTF_8),
                URLEncoder.encode(favTeam, StandardCharsets.UTF_8)
        );

        response.sendRedirect(kakaoAuthUrl);
    }

    // 4. 웹용 카카오 콜백
    @GetMapping("/web/kakao/callback")
    public void webKakaoCallback(
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

            User user = kakaoService.processKakaoWebLogin(code, favTeam);

            String accessToken = jwtProvider.createAccessToken(user.getId().toString());
            String refreshToken = jwtProvider.createRefreshToken(user.getId().toString());

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

    // 5. 웹 로그인 완료 신호 수신
    @GetMapping("/web/login-completed")
    public ResponseEntity<ApiResponse<?>> webLoginCompleted(
            @RequestParam(required = false) String accessToken,
            @RequestParam(required = false) String refreshToken) {
        try {
            if (accessToken != null && refreshToken != null) {
                AuthResponse authResponse = new AuthResponse(accessToken, refreshToken);
                return ResponseEntity.ok(ApiResponse.success(authResponse));
            }

            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("SIGNAL_ERROR", "신호 처리 실패"));
        }
    }

    // 웹용 성공 페이지 생성
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
                        const response = await fetch('/api/users/me', {
                            headers: { 'Authorization': 'Bearer ' + token }
                        });
                        
                        if (!response.ok) {
                            throw new Error('HTTP ' + response.status + ': ' + response.statusText);
                        }
                        
                        const data = await response.json();
                        document.getElementById('apiResult').innerHTML = 
                            '<h4>API 테스트 결과:</h4><pre style="background:#f5f5f5;padding:10px;border-radius:5px;">' + 
                            JSON.stringify(data, null, 2) + '</pre>';
                    } catch (error) {
                        document.getElementById('apiResult').innerHTML = 
                            '<h4 style="color:red">API 테스트 실패:</h4><p>' + error.message + '</p>';
                    }
                }
                
                localStorage.setItem('access_token', '%s');
                localStorage.setItem('refresh_token', '%s');
                
                fetch('/auth/web/login-completed?accessToken=' + encodeURIComponent('%s') + '&refreshToken=' + encodeURIComponent('%s'), {
                    method: 'GET'
                }).catch(e => console.log('신호 전송:', e));
            
            </script>
        </body>
        </html>
        """,
                user.getNickname(),
                user.getFavTeam(),
                accessToken,
                refreshToken,
                accessToken,
                refreshToken,
                accessToken,
                refreshToken
        );
    }

    @PostMapping("/kakao/check")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkExistingUser(@RequestBody KakaoCheckRequestDto request) {
        try {
            Long kakaoId = kakaoService.getKakaoIdFromToken(request.getAccessToken());
            boolean exists = userRepository.existsByKakaoId(kakaoId);

            Map<String, Boolean> result = Map.of("exists", exists);
            return ResponseEntity.ok(ApiResponse.success("사용자 확인이 완료되었습니다.", result));
        } catch (Exception e) {
            // 카카오 토큰이 유효하지 않거나 통신에 실패한 경우
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("CHECK_ERROR", "사용자 확인 중 오류가 발생했습니다."));
        }
    }
}
