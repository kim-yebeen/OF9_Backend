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
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
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

    //웹/앱 공용 로그인 API
    @PostMapping("/kakao/login")
    public ResponseEntity<ApiResponse<AuthResponse>> kakaoLogin(@RequestBody KakaoLoginRequestDto request, @RequestParam String platform) {
        try {
            User user = kakaoService.processKakaoLogin(request.getToken(), request.getFavTeam(), platform);

            String accessToken = jwtProvider.createAccessToken(user.getId().toString());
            String refreshToken = jwtProvider.createRefreshToken(user.getId().toString());

            AuthResponse authResponse = new AuthResponse(accessToken, refreshToken);
            return ResponseEntity.ok(ApiResponse.success("로그인에 성공했습니다.", authResponse));
        } catch (Exception e) {
            log.error("카카오 로그인 처리 중 오류 발생. Code: {}", request.getToken(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("LOGIN_ERROR", "로그인 처리 중 오류가 발생했습니다."));
        }
    }

    //웹에서 카카오 로그인을 시작하기 위한 리다이렉트 API
    @GetMapping("/kakao/web")
    public void redirectToKakaoAuth(@RequestParam String favTeam, HttpServletResponse response) throws IOException {
        String kakaoAuthUrl = String.format(
                "https://kauth.kakao.com/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code&state=%s",
                kakaoClientId,
                URLEncoder.encode(kakaoWebRedirectUri, StandardCharsets.UTF_8),
                URLEncoder.encode(favTeam, StandardCharsets.UTF_8)
        );
        response.sendRedirect(kakaoAuthUrl);
    }
    @GetMapping("/web/kakao/callback")
    public ResponseEntity<String> kakaoWebRedirectForTest(@RequestParam String code) {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>인증 코드 확인</title>
                <style>
                    body { font-family: sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; background-color: #f0f2f5; }
                    .container { background: white; padding: 40px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); text-align: center; }
                    h2 { color: #333; }
                    .code-box { background: #eee; padding: 15px; border-radius: 4px; word-break: break-all; margin: 20px 0; font-family: monospace; font-size: 1.1em; }
                    button { background: #007bff; color: white; border: none; padding: 10px 15px; border-radius: 4px; cursor: pointer; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>✅ 인증 코드 발급 성공</h2>
                    <p>아래 코드를 복사하여 Postman에서 사용하세요.</p>
                    <div id="code" class="code-box">%s</div>
                    <button onclick="copyCode()">코드 복사</button>
                </div>
                <script>
                    function copyCode() {
                        navigator.clipboard.writeText(document.getElementById('code').textContent);
                        alert('코드가 복사되었습니다!');
                    }
                </script>
            </body>
            </html>
        """;
        return ResponseEntity.ok(String.format(html, code));
    }
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

}
