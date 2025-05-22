package com.nine.baseballdiary.backend.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "nickname"))
@Getter @Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private Long kakaoId;

    @Column(nullable=false, unique=true, length=50)
    private String nickname;

    private String profileImageUrl;
    private String favTeam;

    @Column(nullable=false)
    private Boolean isPrivate = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
