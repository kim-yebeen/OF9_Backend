package com.nine.baseballdiary.backend.record;

import com.nine.baseballdiary.backend.game.Game;
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

    // User 관계는 userId 필드만 사용
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Game 관계는 ManyToOne으로 유지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Game game;

    // 나머지 필드들
    private String seatInfo;
    private String stadium;
    private Integer emotionCode;
    private String comment;
    @Column(columnDefinition = "TEXT")
    private String longContent;
    private String bestPlayer;

    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name = "record_companions", joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "companion_id")
    private List<Long> companions;

    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name = "record_food_tags", joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "tag")
    private List<String> foodTags;

    @ElementCollection(fetch=FetchType.EAGER)
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
