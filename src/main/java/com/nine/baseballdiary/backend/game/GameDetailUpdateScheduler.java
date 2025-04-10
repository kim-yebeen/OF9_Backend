package com.nine.baseballdiary.backend.game;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GameDetailUpdateScheduler {

    private final GameRepository gameRepository;
    private final GameDetailService detailService;

    // 매일 오전 5시에 실행
    @Scheduled(cron = "0/30 * * * * ?")
    public void updateDaily() {
        System.out.println("🕔 상세 정보 갱신 시작");

        List<Game> list = gameRepository.findByStatus("SCHEDULED");

        for (Game game : list) {
            detailService.updateGameDetails(game);
        }

        System.out.println("✅ 상세 정보 갱신 완료");
    }
}
