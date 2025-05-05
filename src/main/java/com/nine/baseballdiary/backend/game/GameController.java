package com.nine.baseballdiary.backend.game;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final GameDetailService detailService;      // ← 직접 주입
    private final GameRepository gameRepo;

    /** 달력 조회 */
    @GetMapping
    public List<GameResponse> listByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return gameRepo.findByDateBetween(from, to)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }


    /** 단일 경기 기본 정보 */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameResponse> getGame(@PathVariable String gameId) {
        Game g = gameService.getGameById(gameId);
        if (g == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDto(g));
    }

    /** 단일 경기 상세 업데이트 (즉시) */
    @GetMapping("/{gameId}/detail/update")
    public ResponseEntity<String> updateDetail(@PathVariable String gameId) {
        detailService.updateGameDetail(gameId);        // ← GameDetailService 직접 호출
        return ResponseEntity.ok("업데이트 요청 완료: " + gameId);
    }

    private GameResponse toDto(Game g) {
        return new GameResponse(
                g.getGameId(),
                g.getDate(),
                g.getTime(),
                g.getPlaytime(),
                g.getStadium(),
                g.getHomeTeam(),
                g.getAwayTeam(),
                g.getHomeScore(),
                g.getAwayScore(),
                g.getStatus()
        );
    }
}
