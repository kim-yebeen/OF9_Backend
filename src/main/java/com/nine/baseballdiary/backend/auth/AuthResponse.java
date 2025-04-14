package com.nine.baseballdiary.backend.auth;

public class AuthResponse {
    private Long userId;
    private String nickname;
    private String token;

    public AuthResponse(Long userId, String nickname, String token) {
        this.userId = userId;
        this.nickname = nickname;
        this.token = token;
    }

    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getToken() { return token; }
}