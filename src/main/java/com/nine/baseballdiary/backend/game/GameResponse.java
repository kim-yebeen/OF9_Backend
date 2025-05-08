package com.nine.baseballdiary.backend.game;

import java.time.LocalDate;
import java.time.LocalTime;

public class GameResponse {
    private String gameId;
    private LocalDate date;
    private LocalTime time;
    private LocalTime playtime;
    private String stadium;
    private String homeTeam;
    private String awayTeam;
    private Integer homeScore;
    private Integer awayScore;
    private String status;

    public GameResponse(String gameId,
                        LocalDate date,
                        LocalTime time,
                        LocalTime playtime,
                        String stadium,
                        String homeTeam,
                        String awayTeam,
                        Integer homeScore,
                        Integer awayScore,
                        String status) {
        this.gameId    = gameId;
        this.date      = date;
        this.time      = time;
        this.playtime  = playtime;
        this.stadium   = stadium;
        this.homeTeam  = homeTeam;
        this.awayTeam  = awayTeam;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.status    = status;
    }

    // ─── getters ─────────────────────────────────────────────────────────────
    public String getGameId()   { return gameId; }
    public LocalDate getDate()  { return date; }
    public LocalTime getTime()  { return time; }
    public LocalTime getPlaytime() { return playtime; }
    public String getStadium()  { return stadium; }
    public String getHomeTeam() { return homeTeam; }
    public String getAwayTeam() { return awayTeam; }
    public Integer getHomeScore(){ return homeScore; }
    public Integer getAwayScore(){ return awayScore; }
    public String getStatus()   { return status; }


    public static GameResponse from(Game game) {
        return new GameResponse(
                game.getGameId(),
                game.getDate(),
                game.getTime(),
                game.getPlaytime(),
                game.getStadium(),
                game.getHomeTeam(),
                game.getAwayTeam(),
                game.getHomeScore(),
                game.getAwayScore(),
                game.getStatus()
        );
    }
}
