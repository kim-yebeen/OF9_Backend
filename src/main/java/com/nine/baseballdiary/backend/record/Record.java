package com.nine.baseballdiary.backend.record;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "record")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;           // 레코드 ID

    @Column(nullable = false)
    private Long userId;             // 유저 ID

    @Column(nullable = false)
    private String gameId;           // 경기 ID

    private String seatInfo;         // 좌석 정보
    private String stadium;   // 티켓 이미지 URL
    private Integer emotionCode;     // 감정 코드
    private String comment;          // 한줄평
    @Column(columnDefinition = "TEXT")
    private String longContent;      // 긴 본문 텍스트

    private String bestPlayer;       // 베스트 플레이어

    // 함께 한 친구들
    @ElementCollection
    @CollectionTable(name = "record_companions", joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "companion")
    private List<String> companions; // 함께 한 친구들

    @ElementCollection
    @CollectionTable(name = "record_food_tags", joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "tag")
    private List<String> foodTags;   // 음식 태그

    @ElementCollection
    @CollectionTable(name = "record_media_urls", joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "url")
    private List<String> mediaUrls;  // 미디어 URL 리스트

    private String result;           // 경기 결과 (WIN/LOSE/DRAW)

    private LocalDateTime createdAt;  // 생성일 수동 설정
    private LocalDateTime updatedAt;  // 수정일 수동 설정

    // Getters, Setters
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // 생성 시, createdAt 자동 설정
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;  // 처음 생성 시, createdAt과 동일하게 설정
    }

    // 업데이트 시, updatedAt 자동 설정
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}