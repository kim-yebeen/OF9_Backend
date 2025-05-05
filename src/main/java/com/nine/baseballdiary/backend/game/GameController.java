package com.nine.baseballdiary.backend.game;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameDetailService gameDetailService;

    /** 개별 게임 상세 정보 즉시 업데이트 */
    @GetMapping("/{gameId}/detail/update")
    public ResponseEntity<String> updateGameDetail(@PathVariable String gameId) {
        gameDetailService.updateGameDetail(gameId);
        return ResponseEntity.ok("업데이트 요청 완료: " + gameId);
    }
}