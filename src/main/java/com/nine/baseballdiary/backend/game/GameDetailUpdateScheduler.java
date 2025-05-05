// src/main/java/com/nine/baseballdiary/backend/game/GameDetailUpdateScheduler.java
package com.nine.baseballdiary.backend.game;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GameDetailUpdateScheduler {

    private static final Logger log = LoggerFactory.getLogger(GameDetailUpdateScheduler.class);
    private final GameRepository repo;
    private final GameDetailService detailSvc;

    /** 오늘(포함) 이전의 SCHEDULED·CANCELLED 경기만 재검증 (매일 16시) */
    @Scheduled(cron = "0 25 16 * * ?")
    public void dailyUpdate() {
        LocalDate today = LocalDate.now();
        List<Game> toUpdate = repo.findByStatusAndDateLessThanEqual("SCHEDULED", today);
        toUpdate.addAll(repo.findByStatusAndDateLessThanEqual("CANCELLED", today));
        log.info("🔄 재검증 대상: {}건", toUpdate.size());
        toUpdate.forEach(detailSvc::updateGameDetails);
        log.info("✅ 재검증 완료");
    }
}
