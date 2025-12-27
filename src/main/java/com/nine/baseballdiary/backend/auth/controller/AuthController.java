package com.nine.baseballdiary.backend.auth.controller;

import com.nine.baseballdiary.backend.auth.dto.request.AppleLoginRequestDto;
import com.nine.baseballdiary.backend.auth.dto.request.KakaoCheckRequestDto;
import com.nine.baseballdiary.backend.auth.dto.request.KakaoLoginRequestDto;
import com.nine.baseballdiary.backend.auth.dto.request.RefreshTokenRequest;
import com.nine.baseballdiary.backend.auth.dto.response.AuthResponse;
import com.nine.baseballdiary.backend.auth.security.JwtProvider;
import com.nine.baseballdiary.backend.auth.service.AppleService;
import com.nine.baseballdiary.backend.auth.service.KakaoService;
import com.nine.baseballdiary.backend.auth.service.RefreshTokenService;
import com.nine.baseballdiary.backend.common.response.ApiResponse;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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

    private final AppleService appleService;
    private final KakaoService kakaoService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.web.redirect-uri}")
    private String kakaoWebRedirectUri;
    @PostMapping("/apple/login")
    public ResponseEntity<ApiResponse<AuthResponse>> appleLogin(@RequestBody AppleLoginRequestDto request) {
        try {
            User user = appleService.processAppleLogin(
                    request.getIdentityToken(),
                    request.getUser(),
                    request.getFavTeam()
            );

            String userId = user.getId().toString();
            String accessToken = jwtProvider.createAccessToken(userId);
            String refreshToken = jwtProvider.createRefreshToken(userId);
            refreshTokenService.saveRefreshToken(userId, refreshToken);

            AuthResponse authResponse = new AuthResponse(accessToken, refreshToken);
            return ResponseEntity.ok(ApiResponse.success("애플 로그인 성공", authResponse));
        } catch (Exception e) {
            log.error("애플 로그인 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("LOGIN_ERROR", "애플 로그인 실패"));
        }
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

    //웹/앱 공용 로그인 API
    @PostMapping("/kakao/login")
    public ResponseEntity<ApiResponse<AuthResponse>> kakaoLogin(@RequestBody KakaoLoginRequestDto request, @RequestParam String platform) {
        try {
            User user = kakaoService.processKakaoLogin(request.getToken(), request.getFavTeam(), platform);
            String userId=user.getId().toString();

            //String accessToken = jwtProvider.createAccessToken(user.getId().toString());
            //String refreshToken = jwtProvider.createRefreshToken(user.getId().toString());
            String accessToken = jwtProvider.createAccessToken(userId);
            String refreshToken = jwtProvider.createRefreshToken(userId);
            refreshTokenService.saveRefreshToken(userId,refreshToken);

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
    public void kakaoWebRedirect(@RequestParam String code, HttpServletResponse response) throws IOException {

        // 1. 프론트 Manifest에 설정한 scheme(dodada)과 host(callback)를 정확히 입력
        // 2. 쿼리 파라미터로 code를 붙여서 앱에 전달
        String appUrl = "dodada://callback?code=" + code;

        log.info("App으로 리다이렉트 시도: {}", appUrl);

        // 3. 리다이렉트 전송 (앱이 설치되어 있다면 앱이 켜짐)
        response.sendRedirect(appUrl);
    }

    /*
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
    */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            // 1. 요청받은 Refresh Token에서 userId 추출 (JWT 서명 검증 포함)
            String userId = jwtProvider.getUserIdFromToken(request.getRefreshToken());

            // 2. Redis에 저장된 토큰 가져오기
            String savedRefreshToken = refreshTokenService.getRefreshToken(userId);

            // 3. Redis에 토큰이 없거나, 요청온 토큰과 다르면 에러 (보안 핵심!)
            //    -> 이미 로그아웃된 유저거나, 탈취된 토큰을 사용하는 경우 차단 가능
            if (savedRefreshToken == null || !savedRefreshToken.equals(request.getRefreshToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("INVALID_TOKEN", "유효하지 않거나 만료된 토큰입니다."));
            }

            // 4. (RTR 적용) 새로운 토큰 발급
            String newAccessToken = jwtProvider.createAccessToken(userId);
            String newRefreshToken = jwtProvider.createRefreshToken(userId);

            // 5. Redis 값 갱신 (기존 것 덮어쓰기)
            refreshTokenService.saveRefreshToken(userId, newRefreshToken);

            return ResponseEntity.ok(ApiResponse.success(new AuthResponse(newAccessToken, newRefreshToken)));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("INVALID_REFRESH_TOKEN", "토큰 검증 실패"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String accessToken) {
        // 1. Access Token에서 userId 추출 (Bearer 제거)
        String token = accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
        String userId = jwtProvider.getUserIdFromToken(token);

        // 2. Redis에서 해당 유저의 Refresh Token 삭제
        refreshTokenService.deleteRefreshToken(userId);

        // 3. (선택) Access Token을 Redis 블랙리스트에 등록할 수도 있음 (더 강력한 보안 필요 시)

        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다."));
    }

}
