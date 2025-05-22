package com.nine.baseballdiary.backend.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "follow_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne @JoinColumn(name = "target_id", nullable = false)
    private User target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowRequestStatus status;

    // 생성 시점 타임스탬프 자동 할당
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수정 시점 타임스탬프 자동 할당
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;}