package com.nine.baseballdiary.backend.emotion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "emotion")
@Getter
@NoArgsConstructor
public class Emotion {

    @Id
    @Column(name = "code")
    private Integer code;

    @Column(name = "label", nullable = false, unique = true, length = 20)
    private String label;
}
