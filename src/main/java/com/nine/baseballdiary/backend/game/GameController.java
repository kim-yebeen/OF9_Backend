package com.nine.baseballdiary.backend.game;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;

    /** 기간검색: /games?from=2025-05-01&to=2025-05-31 */
    @GetMapping
    public ResponseEntity<List<GameResponse>> getGames(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<Game> games = gameService.getGamesByDateRange(from, to);
        List<GameResponse> dto = games.stream()
                .map(GameResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dto);
    }

    /** 단일 조회: /games/{id} */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameResponse> getGame(@PathVariable String gameId) {
        Game g = gameService.getGameById(gameId);
        return ResponseEntity.ok(GameResponse.fromEntity(g));
    }

    @GetMapping("/search")
    public ResponseEntity<GameResponse> searchGame(
            @RequestParam("awayTeam") String awayTeam,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("time") String timeStr
    ) {
        LocalTime time = LocalTime.parse(timeStr);
        Game game = gameService.findGameByCondition(awayTeam, date, time);
        return ResponseEntity.ok(GameResponse.fromEntity(game));
    }
}
