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

    /**
     * 매일 오후 10시에
     * 오늘까지의 SCHEDULED 경기만 재검증
     */
    @Scheduled(cron = "0 0 22 * * ?")
    public void recheckScheduled() {
        LocalDate today = LocalDate.now();
        List<Game> toUpdate = repo.findByStatusAndDateLessThanEqual("SCHEDULED", today);
        log.info("🔄 재검증 대상 (SCHEDULED ≤ 오늘): {}건", toUpdate.size());

        toUpdate.forEach(detailSvc::updateGameDetails);
        log.info("✅ 재검증 완료");
    }
}
