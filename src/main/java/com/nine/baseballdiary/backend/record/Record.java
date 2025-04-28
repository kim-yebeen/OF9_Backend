package com.nine.baseballdiary.backend.record;

import jakarta.persistence.*;
import lombok.*;
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
    private Integer recordId;

    // JWT에서 추출한 사용자 ID
    @Column(nullable = false)
    private Integer userId;

    // KBO game 테이블의 PK (예: "20250401DSNC0")
    @Column(nullable = false)
    private String gameId;

    // OCR로 추출 또는 사용자가 보정하는 경기 정보
    private LocalDate gameDate;
    private String homeTeam;
    private String awayTeam;
    private LocalTime startTime;

    // 좌석 정보 (옵셔널)
    private String seatInfo;

    // 세부 입력 (emotionEmoji는 필수)
    @Column(nullable = false)
    private String emotionEmoji;
    private String comment;
    private String bestPlayer;

    @ElementCollection
    @CollectionTable(name = "record_food_tags", joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "tag")
    private List<String> foodTags;

    @ElementCollection
    @CollectionTable(name = "record_media_urls", joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "url")
    private List<String> mediaUrls;

    private String result;      // WIN / LOSE / DRAW

    @Column(nullable = false)
    private String status = "DRAFT";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
