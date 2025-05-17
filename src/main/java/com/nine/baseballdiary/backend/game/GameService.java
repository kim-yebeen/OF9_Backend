package com.nine.baseballdiary.backend.game;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepo;

    /**
     * 날짜 범위(from/to) 내의 모든 경기 조회
     */
    public List<Game> getGamesByDateRange(LocalDate from, LocalDate to) {
        return gameRepo.findByDateBetween(from, to);
    }

    /**
     * 단일 경기 조회 (예외를 던짐)
     */
    public Game getGameById(String gameId) {
        return gameRepo.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 경기 ID: " + gameId));
    }

    /**
     * Optional로 단일 경기 조회
     */
    public Optional<Game> findById(String gameId) {
        return gameRepo.findById(gameId);
    }

    /**
     * 존재 여부 체크
     */
    public boolean existsById(String gameId) {
        return gameRepo.existsById(gameId);
    }

    /**
     * 단순 저장 메서드 (insert or update)
     */
    public void saveGame(Game game) {
        gameRepo.save(game);
    }

    /**
     * 1차 크롤러용: 스케줄 정보만 신규/업데이트
     */
    @Transactional
    public void saveOrUpdateSchedule(Game incoming) {
        String id = incoming.getGameId();
        if (gameRepo.existsById(id)) {
            Game existing = gameRepo.findById(id).get();
            // 이미 종료된 경기면 건너뜀
            if ("FINISHED".equals(existing.getStatus())) {
                return;
            }
            // 스케줄 정보만 갱신
            existing.setDate(incoming.getDate());
            existing.setTime(incoming.getTime());
            existing.setStadium(incoming.getStadium());
            existing.setHomeTeam(incoming.getHomeTeam());
            existing.setAwayTeam(incoming.getAwayTeam());
            existing.setStatus(incoming.getStatus());
            gameRepo.save(existing);
        } else {
            // 신규 삽입
            gameRepo.save(incoming);
        }
    }

    /**
     * 2차 크롤러용: 점수·상태 업데이트
     */
    @Transactional
    public void updateResult(Game incoming) {
        String id = incoming.getGameId();
        Game existing = gameRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 경기 ID: " + id));
        // 이미 종료된 경기면 무시
        if ("FINISHED".equals(existing.getStatus())) {
            return;
        }
        existing.setHomeScore(incoming.getHomeScore());
        existing.setAwayScore(incoming.getAwayScore());
        existing.setStatus(incoming.getStatus());
        gameRepo.save(existing);
    }

    public Game findGameByCondition(String awayTeam, LocalDate date, LocalTime time) {
        return gameRepo.findByAwayTeamAndDateAndTime(awayTeam, date, time)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 경기 정보를 찾을 수 없습니다."));
    }
}

