package com.nine.baseballdiary.backend.game;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameDetailService {

    private static final Logger log = LoggerFactory.getLogger(GameDetailService.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");

    private final GameRepository    gameRepository;
    private final RestTemplate      restTemplate;
    private final ObjectMapper      objectMapper;

    /**
     * 개별 호출 시
     */
    public void updateGameDetail(String gameId, String unused) {
        Optional<Game> opt = gameRepository.findById(gameId);
        if (opt.isEmpty()) {
            log.warn("DB에 없음: {}", gameId);
            return;
        }
        _update(opt.get());
    }

    /**
     * 스케줄러용: TIME·STADIUM·STATUS가 모두 들어 있고
     * 이미 FINISHED/CANCELLED 면 건너뛰기
     */
    public void updateGameDetails(Game g) {
        String st = g.getStatus();
        if (g.getStadium() != null
                && g.getTime()    != null
                && ("FINISHED".equals(st) || "CANCELLED".equals(st))) {
            log.debug("스킵: {}", g.getGameId());
            return;
        }
        _update(g);
    }

    @Transactional
    void _update(Game game) {
        String season = String.valueOf(game.getDate().getYear());
        String url = "https://www.koreabaseball.com/ws/Schedule.asmx/GetScoreBoardScroll"
                + "?leId=1&srId=0&seasonId=" + season
                + "&gameId="    + game.getGameId();

        try {
            HttpHeaders h = new HttpHeaders();
            h.add("User-Agent", "Mozilla/5.0");
            h.add("Referer", "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx");
            HttpEntity<Void> req = new HttpEntity<>(h);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, req, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.error("상세API 오류 [{}]", resp.getStatusCodeValue());
                return;
            }

            JsonNode root = objectMapper.readTree(resp.getBody());
            if (root.has("d")) {
                root = objectMapper.readTree(root.get("d").asText());
            }

            // 1) 취소 감지 (CANCEL_SC_NM)
            String cancelNm = root.path("CANCEL_SC_NM").asText("");
            if (!cancelNm.isBlank()) {
                game.setStatus("CANCELLED");
                gameRepository.save(game);
                log.info("  ▶ CANCELLED: {}", game.getGameId());
                return;
            }

            // 2) 구장
            if (root.has("S_NM")) {
                game.setStadium(root.get("S_NM").asText());
            }

            // 3) 시작 시간
            String stime = root.path("START_TM").asText("");
            if (isValid(stime)) {
                game.setTime(LocalTime.parse(stime, TIME_FMT));
            }

            // 4) 소요 시간
            String uime = root.path("USE_TM").asText("");
            if (isValid(uime)) {
                game.setPlaytime(LocalTime.parse(uime, TIME_FMT));
            }

            // 5) 점수 (table3)
            if (root.has("table3")) {
                String tbl3 = root.get("table3").asText().replaceAll("\r\n", "");
                if (!tbl3.isBlank()) {
                    JsonNode rows = objectMapper.readTree(tbl3).path("rows");
                    int away = rows.get(0).path("row").get(0).path("Text").asInt(0);
                    int home = rows.get(1).path("row").get(0).path("Text").asInt(0);
                    game.setAwayScore(away);
                    game.setHomeScore(home);
                }
            }

            // 6) 상태 FINISHED
            if (game.getHomeScore() != null && game.getAwayScore() != null) {
                game.setStatus("FINISHED");
            }

            gameRepository.save(game);
            log.info("  ▶ FINISHED: {}", game.getGameId());

        } catch (Exception ex) {
            log.error("상세API 예외: {} - {}", game.getGameId(), ex.getMessage(), ex);
        }
    }

    private boolean isValid(String s) {
        return s != null && !s.isBlank() && !"null".equalsIgnoreCase(s);
    }
}
