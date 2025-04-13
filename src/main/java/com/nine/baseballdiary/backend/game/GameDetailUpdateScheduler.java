package com.nine.baseballdiary.backend.game;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GameDetailUpdateScheduler {

    private static final Logger log = LoggerFactory.getLogger(GameDetailUpdateScheduler.class);
    private final GameRepository gameRepository;
    private final GameDetailService detailService;

    // 스케줄러 주기를 필요에 따라 조정하거나 조건에 맞는 게임만 업데이트하도록 수정
    @Scheduled(cron = "0 0/15 * * * ?") // 예: 매 15분마다 업데이트
    public void updateDaily() {
        log.info("🕔 상세 정보 갱신 시작");
        List<Game> list = gameRepository.findByStatus("FINISHED");
        for (Game game : list) {
            detailService.updateGameDetails(game);
        }
        log.info("✅ 상세 정보 갱신 완료");
    }
}

