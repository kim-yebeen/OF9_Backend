package com.nine.baseballdiary.backend.reaction;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reaction_type")
@Getter
@NoArgsConstructor
public class ReactionType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String name;

    private String emoji;

    @Column(name = "display_order")
    private Integer displayOrder;
}