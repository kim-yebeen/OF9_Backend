package com.nine.baseballdiary.backend.game;

import java.time.LocalDate;
import java.time.LocalTime;

public class GameResponse {
    private String gameId;
    private LocalDate date;
    private LocalTime time;
    private String stadium;
    private String homeTeam;
    private String awayTeam;
    private Integer homeScore;
    private Integer awayScore;
    private String status;

    // 생성자
    public GameResponse(String gameId, LocalDate date, LocalTime time,
                        String stadium, String homeTeam, String awayTeam,
                        Integer homeScore, Integer awayScore, String status) {
        this.gameId = gameId;
        this.date = date;
        this.time = time;
        this.stadium = stadium;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.status = status;
    }

    // 엔티티 → DTO 변환 헬퍼
    public static GameResponse fromEntity(Game g) {
        return new GameResponse(
                g.getGameId(),
                g.getDate(),
                g.getTime(),
                g.getStadium(),
                g.getHomeTeam(),
                g.getAwayTeam(),
                g.getHomeScore(),
                g.getAwayScore(),
                g.getStatus()
        );
    }

    // getter / setter 생략 (lombok @Getter/@Setter 써도 됩니다)
}
