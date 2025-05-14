package com.nine.baseballdiary.backend.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 기본 생성자
    public User() {}

    // Long 타입 ID를 받는 생성자 추가
    public User(Long id) {
        this.id = id;
    }
    @Column(nullable = false, unique = true)
    private Long kakaoId;

    private String nickname;

    private String favTeam; // ✅ 좋아하는 팀 필드 추가

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getKakaoId() { return kakaoId; }
    public void setKakaoId(Long kakaoId) { this.kakaoId = kakaoId; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getFavTeam() { return favTeam; }
    public void setFavTeam(String favTeam) { this.favTeam = favTeam; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    public String getProfileImageUrl() {
        return profileImageUrl;
    }
}
