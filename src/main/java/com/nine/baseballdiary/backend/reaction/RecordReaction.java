package com.nine.baseballdiary.backend.reaction;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "record_reaction")
@Getter
@NoArgsConstructor
public class RecordReaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reaction_type_id", nullable = false)
    private Integer reactionTypeId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public RecordReaction(Long recordId, Long userId, Integer reactionTypeId) {
        this.recordId = recordId;
        this.userId = userId;
        this.reactionTypeId = reactionTypeId;
        this.createdAt = LocalDateTime.now();
    }
    public void updateReactionType(Integer reactionTypeId) {
        this.reactionTypeId = reactionTypeId;
    }
}