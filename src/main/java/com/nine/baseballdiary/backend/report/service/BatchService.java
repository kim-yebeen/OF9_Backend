package com.nine.baseballdiary.backend.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {

    private final BadgeService badgeService;
    // private final RankingService rankingService; // 랭킹 계산 서비스 (추후 구현)

    /**
     * 매일 새벽 4시에 뱃지 부여 및 랭킹 업데이트 작업을 실행합니다.
     * cron = "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void scheduleDailyTasks() {
        log.info("===== [BATCH] 일일 배치 작업 시작 =====");

        try {
            log.info("[BATCH] 전체 사용자 뱃지 부여 작업을 시작합니다.");
            badgeService.checkAndAwardBadgesForAllUsers();
            log.info("[BATCH] 전체 사용자 뱃지 부여 작업을 완료했습니다.");

            // [주석 처리] 추후 랭킹 기능 구현 시 활성화
            // log.info("[BATCH] 전체 사용자 랭킹 업데이트 작업을 시작합니다.");
            // rankingService.updateAllUserRankings();
            // log.info("[BATCH] 전체 사용자 랭킹 업데이트 작업을 완료했습니다.");

        } catch (Exception e) {
            log.error("[BATCH] 일일 배치 작업 중 심각한 오류가 발생했습니다.", e);
        }

        log.info("===== [BATCH] 일일 배치 작업 완료 =====");
    }
}