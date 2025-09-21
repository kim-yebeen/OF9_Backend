package com.nine.baseballdiary.backend.record;

import com.nine.baseballdiary.backend.game.Game;
import com.nine.baseballdiary.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "record")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GameRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;

    // --- User 관계 수정 ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // 실제 DB 컬럼의 주인
    private User user;

    @Column(name = "user_id", insertable = false, updatable = false) // DB에 쓰기 작업은 하지 않음
    private Long userId;

    // --- Game 관계 수정 ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id") // 실제 DB 컬럼의 주인
    private Game game;

    @Column(name = "game_id", insertable = false, updatable = false) // DB에 쓰기 작업은 하지 않음
    private String gameId;

    // --- 나머지 필드는 기존과 동일 ---
    private String seatInfo;
    private String stadium;
    private Integer emotionCode;
    private String comment;
    @Column(columnDefinition = "TEXT")
    private String longContent;
    private String bestPlayer;

    @ElementCollection
    @CollectionTable(name = "record_companions", joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "companion_id")
    private List<Long> companions;

    @ElementCollection
    @CollectionTable(name = "record_food_tags", joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "tag")
    private List<String> foodTags;

    @ElementCollection
    @CollectionTable(name = "record_media_urls", joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "url", columnDefinition = "TEXT")
    private List<String> mediaUrls;

    private String result;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}