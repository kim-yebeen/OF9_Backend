package com.nine.baseballdiary.backend.record;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "record")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer recordId;

    @Column(nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private String gameId;       // FK to game.game_id

    //OCR 로 추출되거나 사용자가 수정할 수 있는 경기 일자/팀/시간 정보
    private String gameDate;     // "2025-07-01" 형태
    private String homeTeam;
    private String awayTeam;
    private String startTime;    // "14:00"

    // 업로드 된 티켓 이미지 URL
    private String ticketImageUrl;
    //좌석 정보
    private String seatInfo;

    private String comment;
    private String emotionEmoji;
    private String bestPlayer;

    //복수 먹거리 태그
    @ElementCollection
    @CollectionTable(name = "record_food_tags", joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "tag")
    private List<String> foodTags;

    //복수 미디어 URL
    @ElementCollection
    @CollectionTable(name = "record_media_urls", joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "url")
    private List<String> mediaUrls;

    // 경기 결과
    private String result;   // WIN / LOSE / DRAW

    // 초기 상태, 완료 상태
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
