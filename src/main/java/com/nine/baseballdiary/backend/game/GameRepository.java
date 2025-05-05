package com.nine.baseballdiary.backend.game;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, String> {
    // 기존
    List<Game> findByStatus(String status);

    // 오늘까지의 SCHEDULED 경기만 조회
    List<Game> findByStatusAndDateLessThanEqual(String status, LocalDate date);

    // (1) 달력 API: from/to 기간 동안의 경기
    List<Game> findByDateBetween(LocalDate from, LocalDate to);

    // (3) 홈팀 목록 (distinct)
    @Query("select distinct g.homeTeam from Game g order by g.homeTeam")
    List<String> findDistinctHomeTeam();

    // (4) 특정 홈팀의 원정팀 목록
    @Query("select distinct g.awayTeam from Game g where g.homeTeam = :homeTeam order by g.awayTeam")
    List<String> findDistinctAwayTeamByHomeTeam(@Param("homeTeam") String homeTeam);


}
