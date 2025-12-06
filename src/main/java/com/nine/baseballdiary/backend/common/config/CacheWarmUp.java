package com.nine.baseballdiary.backend.common.config;

import com.nine.baseballdiary.backend.game.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmUp implements ApplicationRunner {

    private final GameService gameService;

    @Override
    public void run(ApplicationArguments args) {
        // 로컬 개발 환경(dev)이나 테스트일 때는 굳이 워밍업 안 해도 되면 프로필 체크 로직 추가 가능
        // if (!profile.equals("prod")) return;

        log.info("🚀 [Cache WarmUp] 서버 시작: 게임 데이터 캐싱을 시작합니다...");

        // 2025년 3월 ~ 11월 데이터 미리 조회 -> Redis 적재
        String year = "2025";
        String[] months = {"03", "04", "05", "06", "07", "08", "09", "10", "11"};

        long start = System.currentTimeMillis();
        for (String month : months) {
            String yearMonth = year + "-" + month;
            try {
                // 이 메서드가 실행될 때 @Cacheable이 동작해서 Redis에 저장됨
                gameService.getGamesByMonth(yearMonth);
            } catch (Exception e) {
                log.warn("⚠️ {} 데이터 워밍업 실패: {}", yearMonth, e.getMessage());
            }
        }
        long end = System.currentTimeMillis();

        log.info("✅ [Cache WarmUp] 완료! (소요시간: {}ms)", (end - start));
    }
}