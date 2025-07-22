package com.nine.baseballdiary.backend.auth;

public class KakaoLoginRequestDto {
    private String accessToken;
    private String favTeam;

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