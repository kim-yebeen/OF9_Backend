package com.nine.baseballdiary.backend.game;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "game")
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

    // getters and setters

    public String getGameId() {
        return gameId;
    }
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public LocalDate getDate() {
        return date;
    }
    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }
    public void setTime(LocalTime time) {
        this.time = time;
    }

    public LocalTime getPlaytime() {
        return playtime;
    }
    public void setPlaytime(LocalTime playtime) {
        this.playtime = playtime;
    }

    public String getStadium() {
        return stadium;
    }
    public void setStadium(String stadium) {
        this.stadium = stadium;
    }

    public String getHomeTeam() {
        return homeTeam;
    }
    public void setHomeTeam(String homeTeam) {
        this.homeTeam = homeTeam;
    }

    public String getAwayTeam() {
        return awayTeam;
    }
    public void setAwayTeam(String awayTeam) {
        this.awayTeam = awayTeam;
    }

    public Integer getHomeScore() {
        return homeScore;
    }
    public void setHomeScore(Integer homeScore) {
        this.homeScore = homeScore;
    }

    public Integer getAwayScore() {
        return awayScore;
    }
    public void setAwayScore(Integer awayScore) {
        this.awayScore = awayScore;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getHomeImg() {
        return homeImg;
    }
    public void setHomeImg(String homeImg) {
        this.homeImg = homeImg;
    }

    public String getAwayImg() {
        return awayImg;
    }
    public void setAwayImg(String awayImg) {
        this.awayImg = awayImg;
    }
}
