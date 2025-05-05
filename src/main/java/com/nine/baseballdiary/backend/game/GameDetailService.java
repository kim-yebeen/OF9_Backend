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

    /**
     * 즉시 개별 게임 상세 갱신
     */
    public void updateGameDetail(String gameId) {
        Optional<Game> opt = gameRepository.findById(gameId);
        if (opt.isEmpty()) {
            log.warn("DB에 gameId={} 없음, 스킵", gameId);
            return;
        }
        updateGame(opt.get());
    }

    /**
     * 스케줄러용: SCHEDULED 또는 CANCELLED/FINISHED 만 업데이트
     */
    public void updateGameDetails(Game game) {
        String st = game.getStatus();
        // 이미 완료/취소 상태인데 구장·시간도 채워져 있으면 스킵
        if (game.getStadium() != null
                && game.getTime() != null
                && ("FINISHED".equals(st) || "CANCELLED".equals(st))) {
            log.debug("이미 완료/취소 상태, 스킵: {}", game.getGameId());
            return;
        }
        updateGame(game);
    }

    /** 내부 갱신 로직 */
    private void updateGame(Game game) {
        String season = String.valueOf(game.getDate().getYear());
        String url = String.format(
                "https://www.koreabaseball.com/ws/Schedule.asmx/GetScoreBoardScroll"
                        + "?leId=1&srId=0&seasonId=%s&gameId=%s",
                season, game.getGameId()
        );

        try {
            // 헤더 셋업
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0");
            headers.set("Referer", "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx");
            HttpEntity<Void> reqEntity = new HttpEntity<>(headers);

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, reqEntity, String.class
            );
            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.error("상세API 오류 [{}]: {}", game.getGameId(), resp.getStatusCodeValue());
                return;
            }

            // JSON 파싱
            JsonNode root = objectMapper.readTree(resp.getBody());
            if (root.has("d")) {
                root = objectMapper.readTree(root.get("d").asText());
            }

            // 1) 점수(table3) 파싱
            int awayScore = 0, homeScore = 0;
            if (root.has("table3")) {
                String t3 = root.get("table3").asText().replaceAll("\r\n", "");
                if (!t3.isBlank()) {
                    JsonNode rows = objectMapper.readTree(t3).path("rows");
                    JsonNode awayRow = rows.get(0).path("row");
                    JsonNode homeRow = rows.get(1).path("row");
                    awayScore = awayRow.isArray() && awayRow.size()>0
                            ? awayRow.get(0).path("Text").asInt(0) : 0;
                    homeScore = homeRow.isArray() && homeRow.size()>0
                            ? homeRow.get(0).path("Text").asInt(0) : 0;
                }
            }
            game.setAwayScore(awayScore);
            game.setHomeScore(homeScore);

            // 2) 소요시간 USE_TM
            String useTm = root.path("USE_TM").asText("");
            LocalTime playtime = isValid(useTm)
                    ? LocalTime.parse(useTm, TIME_FMT)
                    : null;
            game.setPlaytime(playtime);

            // 3) 취소/완료/예정 상태 판별
            if (playtime == null && awayScore == 0 && homeScore == 0) {
                game.setStatus("CANCELLED");
            } else if (awayScore > 0 || homeScore > 0) {
                game.setStatus("FINISHED");
            } else {
                game.setStatus("SCHEDULED");
            }

            // 4) 구장명·시작시간 갱신 (취소경기도 갱신해주면 추후 변경 반영 용)
            if (root.has("S_NM")) {
                game.setStadium(root.get("S_NM").asText());
            }
            String startTm = root.path("START_TM").asText("");
            if (isValid(startTm)) {
                game.setTime(LocalTime.parse(startTm, TIME_FMT));
            }

            // 저장
            gameRepository.save(game);
            log.info("▶ 상세업데이트 완료 [{} → {}]", game.getGameId(), game.getStatus());

        } catch (Exception ex) {
            log.error("상세API 예외: {} - {}", game.getGameId(), ex.getMessage(), ex);
        }
    }

    private boolean isValid(String s) {
        return s != null && !s.isBlank() && !"null".equalsIgnoreCase(s);
    }
}
