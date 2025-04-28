package com.nine.baseballdiary.backend.game;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, String> {
    // 기존
    List<Game> findByStatus(String status);

    // 오늘까지의 SCHEDULED 경기만 조회
    List<Game> findByStatusAndDateLessThanEqual(String status, LocalDate date);
}