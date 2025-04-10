package com.nine.baseballdiary.backend.game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GameController {

    @Autowired
    private GameDetailService gameDetailService;

    // 예시: /api/game-detail/update?gameId=20250401DSNC0&gameDate=20250401
    @GetMapping("/api/game-detail/update")
    public String updateGameDetail(@RequestParam String gameId, @RequestParam String gameDate) {
        gameDetailService.updateGameDetail(gameId, gameDate);
        return "업데이트 요청 완료: " + gameId;
    }
}
