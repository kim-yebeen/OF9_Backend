package com.nine.baseballdiary.backend.game;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;


// ✅ 추가
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class GameService {

    // ✅ 수정
    //@Autowired
    //private GameRepository gameRepository;
    private final GameRepository gameRepository;


    public Game findGameByCondition(String awayTeam, LocalDate date, LocalTime time) {
        return gameRepository.findByAwayTeamAndDateAndTime(awayTeam, date, time)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 경기 정보를 찾을 수 없습니다."));
    }
    public void saveGame(Game game) {
        gameRepository.save(game);
    }

    public Game getGameById(String gameId) {
        return gameRepository.findById(gameId).orElse(null);
    }

    public void updateGame(Game game) {
        gameRepository.save(game);
    }


}
