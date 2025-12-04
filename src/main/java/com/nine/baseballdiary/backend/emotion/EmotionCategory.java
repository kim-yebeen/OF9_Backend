package com.nine.baseballdiary.backend.emotion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "emotion_category")
@Getter
@NoArgsConstructor
public class EmotionCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emotion_code", nullable = false)
    private Integer emotionCode;

    @Column(name = "category", nullable = false, length = 20)
    private String category;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emotion_code", referencedColumnName = "code", insertable = false, updatable = false)
    private Emotion emotion;
}
