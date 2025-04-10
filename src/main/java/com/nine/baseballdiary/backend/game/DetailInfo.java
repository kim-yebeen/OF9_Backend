package com.nine.baseballdiary.backend.game;

public class DetailInfo {
    private String playtime;
    private String stadium;
    private int homeScore;
    private int awayScore;
    private String status;

    // getters and setters

    public String getPlaytime() {
        return playtime;
    }
    public void setPlaytime(String playtime) {
        this.playtime = playtime;
    }
    public String getStadium() {
        return stadium;
    }
    public void setStadium(String stadium) {
        this.stadium = stadium;
    }
    public int getHomeScore() {
        return homeScore;
    }
    public void setHomeScore(int homeScore) {
        this.homeScore = homeScore;
    }
    public int getAwayScore() {
        return awayScore;
    }
    public void setAwayScore(int awayScore) {
        this.awayScore = awayScore;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "DetailInfo{" +
                "playtime='" + playtime + '\'' +
                ", stadium='" + stadium + '\'' +
                ", homeScore=" + homeScore +
                ", awayScore=" + awayScore +
                ", status='" + status + '\'' +
                '}';
    }
}
