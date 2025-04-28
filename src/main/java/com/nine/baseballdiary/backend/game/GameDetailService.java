package com.nine.baseballdiary.backend.game;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameDetailService {

    private static final Logger log = LoggerFactory.getLogger(GameDetailService.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");

    private final RestTemplate restTemplate;
    private final GameRepository gameRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 개별 게임ID로 즉시 상세 갱신 */
    public void updateGameDetail(String gameId, String gameDate) {
        Optional<Game> opt = gameRepository.findById(gameId);
        if (opt.isEmpty()) {
            log.warn("DB에 없음: gameId={}", gameId);
            return;
        }
        updateInternal(opt.get());
    }

    /**
     * 스케줄러에서 호출:
     * TIME/STADIUM/STATUS 가 들어있고 이미 FINISHED/CANCELLED 면 스킵
     */
    public void updateGameDetails(Game g) {
        String status = g.getStatus();
        if (g.getStadium() != null
                && g.getTime()    != null
                && ( "FINISHED".equals(status) || "CANCELLED".equals(status) ))
        {
            log.debug("스킵: {}", g.getGameId());
            return;
        }
        updateInternal(g);
    }

    /** 내부 갱신 로직 */
    private void updateInternal(Game game) {
        String url = String.format(
                "https://www.koreabaseball.com/ws/Schedule.asmx/GetScoreBoardScroll"
                        + "?leId=1&srId=0&seasonId=%s&gameId=%s",
                game.getDate().getYear(), game.getGameId()
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0");
            headers.set("Referer", "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx");
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.error("상세API 오류 [{}]: {}", game.getGameId(), resp.getStatusCodeValue());
                return;
            }

            JsonNode root = objectMapper.readTree(resp.getBody());
            if (root.has("d")) {
                root = objectMapper.readTree(root.get("d").asText());
            }

            // 1) 취소 여부 검사
            int cancelId    = root.path("CANCEL_SC_ID").asInt(0);
            String cancelNm = root.path("CANCEL_SC_NM").asText("");
            if (cancelId != 0 || !cancelNm.isBlank()) {
                game.setStatus("CANCELLED");
                gameRepository.save(game);
                log.info("취소 감지: {}", game.getGameId());
                return;
            }

            // 2) 구장
            if (root.has("S_NM")) {
                game.setStadium(root.get("S_NM").asText());
            }

            // 3) 시작시간
            String stime = root.path("START_TM").asText();
            if (isValid(stime)) {
                game.setTime(LocalTime.parse(stime, TIME_FMT));
            }

            // 4) 소요시간
            String uime = root.path("USE_TM").asText();
            if (isValid(uime)) {
                game.setPlaytime(LocalTime.parse(uime, TIME_FMT));
            }

            // 5) 점수 (table3)
            if (root.has("table3")) {
                String tbl3 = root.get("table3").asText().replaceAll("\r\n", "");
                if (!tbl3.isBlank()) {
                    JsonNode t3 = objectMapper.readTree(tbl3).path("rows");
                    int away = t3.get(0).path("row").get(0).path("Text").asInt(0);
                    int home = t3.get(1).path("row").get(0).path("Text").asInt(0);
                    game.setAwayScore(away);
                    game.setHomeScore(home);
                }
            }

            // 6) 상태: 점수 받았으면 FINISHED
            game.setStatus("FINISHED");
            gameRepository.save(game);
            log.info("완료 처리: {}", game.getGameId());

        } catch (Exception ex) {
            log.error("상세API 예외 [{}]: {}", game.getGameId(), ex.getMessage(), ex);
        }
    }

    private boolean isValid(String s) {
        return s != null && !s.isBlank() && !"null".equalsIgnoreCase(s);
    }
}
