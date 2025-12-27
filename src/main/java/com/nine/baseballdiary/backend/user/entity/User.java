package com.nine.baseballdiary.backend.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "nickname"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //@Column(nullable=false, unique=true)
    //private Long kakaoId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SocialType socialType;
    @Column(nullable = false)
    private String socialId;

    @Column(nullable=false, unique=true, length=50)
    private String nickname;


    @Column(nullable=true, length=500)
    private String profileImageUrl;

    @Column(nullable=false, length=50)
    private String favTeam;

    @Column(nullable=false)
    private Boolean isPrivate = false;

    // ✅ 자동 시간 설정 추가
    @Column(nullable=false, updatable=false)
    private LocalDateTime createdAt;

    @Column(nullable=false)
    private LocalDateTime updatedAt;

    // ✅ JPA 생명주기 콜백 추가
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @Column(name = "push_enabled", nullable = false)
    @Builder.Default
    private Boolean pushEnabled = true;  // 기본값 true (알림 받기)

    public void updatePushEnabled(Boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public Boolean getPushEnabled() {
        return this.pushEnabled;
    }

    @Column(name = "fcm_token")
    private String fcmToken;

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
    public String getFcmToken() {
        return this.fcmToken;
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

