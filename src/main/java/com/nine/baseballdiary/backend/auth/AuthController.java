package com.nine.baseballdiary.backend.auth;

import com.nine.baseballdiary.backend.user.User;
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
        User user = kakaoService.processLogin(request.getAccessToken(), request.getFavTeam());
        String token = jwtProvider.createToken(user.getId().toString());
        return ResponseEntity.ok(new AuthResponse(user.getId(), user.getNickname(), token));
    }
}

