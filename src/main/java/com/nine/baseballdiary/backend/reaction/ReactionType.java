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
    @Column(name = "display_order")
    private Integer displayOrder;  // id 대신 display_order를 Primary Key로

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String name;
}