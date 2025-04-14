package com.nine.baseballdiary.backend.auth;

public class KakaoLoginRequestDto {
    private String accessToken;
    private String favTeam; // ✅ 추가: Flutter에서 선택한 구단명 전달

    public String getAccessToken() {
        return accessToken;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getFavTeam() {
        return favTeam;
    }
    public void setFavTeam(String favTeam) {
        this.favTeam = favTeam;
    }
}