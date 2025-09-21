package com.nine.baseballdiary.backend.badge;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "badge")
@Getter
@NoArgsConstructor
public class Badge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;
    private String imageUrl;
    private Integer threshold;
}