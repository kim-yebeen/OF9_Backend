package com.nine.baseballdiary.backend.like;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "record_like")
@Getter
@NoArgsConstructor
public class RecordLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public RecordLike(Long recordId, Long userId) {
        this.recordId = recordId;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
    }
}
