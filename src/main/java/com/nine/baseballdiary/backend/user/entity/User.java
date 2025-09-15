package com.nine.baseballdiary.backend.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "nickname"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private Long kakaoId;

    @Column(nullable=false, unique=true, length=50)
    private String nickname;

    // ✅ null 허용을 명시적으로 설정
    @Column(nullable=true, length=500)
    private String profileImageUrl;

    // ✅ 필수값으로 설정
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

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
