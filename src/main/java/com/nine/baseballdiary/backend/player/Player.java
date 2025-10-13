package com.nine.baseballdiary.backend.player;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player", indexes = {
        @Index(name = "idx_player_name", columnList = "name")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String team;

    @Column(nullable = false)
    private String position;
}