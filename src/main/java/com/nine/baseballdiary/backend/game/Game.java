package com.nine.baseballdiary.backend.game;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "game")
@Getter @Setter
public class Game {

    @Id
    @Column(name = "game_id")
    private String gameId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "time")
    private LocalTime time;

    @Column(name = "playtime")
    private LocalTime playtime;

    @Column(name = "stadium", length = 50)
    private String stadium;

    @Column(name = "home_team", length = 50)
    private String homeTeam;

    @Column(name = "away_team", length = 50)
    private String awayTeam;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "home_img", columnDefinition = "TEXT")
    private String homeImg;

    @Column(name = "away_img", columnDefinition = "TEXT")
    private String awayImg;

    }
