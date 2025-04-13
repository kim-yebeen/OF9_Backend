package com.nine.baseballdiary.backend.game;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class GameDetailService {

    private static final Logger log = LoggerFactory.getLogger(GameDetailService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private RestTemplate restTemplate; // 빈으로 관리

    public void updateGameDetail(String gameId, String gameDate) {
        Optional<Game> optionalGame = gameRepository.findById(gameId);
        if (optionalGame.isEmpty()) {
            log.warn("DB에서 gameId를 찾을 수 없습니다: {}", gameId);
            return;
        }
        Game game = optionalGame.get();
        updateGame(game, gameDate);
    }

    public void updateGameDetails(Game game) {
        // 이미 필수 정보가 채워져 있다면 업데이트를 건너뛸 수 있음.
        if (game.getStadium() != null && game.getTime() != null) {
            log.debug("gameId {} 이미 업데이트 완료되어 스킵", game.getGameId());
            return;
        }
        String gameDate = game.getDate() != null ? game.getDate().toString().replaceAll("-", "") : "";
        updateGame(game, gameDate);
    }

    private void updateGame(Game game, String gameDate) {
        String seasonId = gameDate.length() >= 4 ? gameDate.substring(0, 4) : "2025";
        String url = String.format(
                "https://www.koreabaseball.com/ws/Schedule.asmx/GetScoreBoardScroll?leId=1&srId=0&seasonId=%s&gameId=%s",
                seasonId, game.getGameId()
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/119.0 Safari/537.36");
            headers.set("Referer", "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx");
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                String responseBody = responseEntity.getBody();

                // 너무 긴 응답은 디버그 레벨에서만 출력
                log.debug("API 응답 (gameId: {}): {}", game.getGameId(), responseBody);

                if (responseBody == null || responseBody.isEmpty()) {
                    log.warn("응답 본문이 비어 있습니다. gameId: {}", game.getGameId());
                    return;
                }

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(responseBody);
                if (rootNode.has("d")) {
                    String innerJson = rootNode.get("d").asText();
                    rootNode = objectMapper.readTree(innerJson);
                }

                // 구장명 업데이트
                if (rootNode.has("S_NM")) {
                    game.setStadium(rootNode.get("S_NM").asText());
                }

                // 시작 시간 업데이트
                String startTimeStr = rootNode.path("START_TM").asText();
                if (isValidTimeString(startTimeStr)) {
                    try {
                        game.setTime(LocalTime.parse(startTimeStr, TIME_FORMATTER));
                    } catch (Exception e) {
                        log.error("시작 시간 파싱 실패 ({}): {}", startTimeStr, e.getMessage());
                    }
                }

                // 경기 소요 시간 업데이트
                String useTmStr = rootNode.path("USE_TM").asText();
                if (isValidTimeString(useTmStr)) {
                    try {
                        game.setPlaytime(LocalTime.parse(useTmStr, TIME_FORMATTER));
                    } catch (Exception e) {
                        log.error("소요 시간 파싱 실패 ({}): {}", useTmStr, e.getMessage());
                    }
                }

                // 점수 업데이트 (table3)
                if (rootNode.has("table3")) {
                    String table3Str = rootNode.get("table3").asText().replaceAll("\r\n", "");
                    if (!table3Str.isBlank()) {
                        JsonNode table3Node = objectMapper.readTree(table3Str);
                        JsonNode rows = table3Node.path("rows");
                        if (rows.isArray() && rows.size() >= 2) {
                            JsonNode awayRow = rows.get(0).path("row");
                            JsonNode homeRow = rows.get(1).path("row");
                            if (awayRow.isArray() && awayRow.size() > 0) {
                                String awayScoreStr = awayRow.get(0).path("Text").asText();
                                try {
                                    game.setAwayScore(Integer.parseInt(awayScoreStr));
                                } catch (Exception e) {
                                    log.error("원정 점수 파싱 실패 ({}): {}", awayScoreStr, e.getMessage());
                                }
                            }
                            if (homeRow.isArray() && homeRow.size() > 0) {
                                String homeScoreStr = homeRow.get(0).path("Text").asText();
                                try {
                                    game.setHomeScore(Integer.parseInt(homeScoreStr));
                                } catch (Exception e) {
                                    log.error("홈 점수 파싱 실패 ({}): {}", homeScoreStr, e.getMessage());
                                }
                            }
                        }
                    }
                }

                // 상태 지정: 0점 경기도 FINISHED로 간주해야 한다면 이 조건을 필요에 따라 수정할 수 있다.
                if ((game.getHomeScore() != null && game.getAwayScore() != null)) {
                    // 단순 비교 없이 API 응답이나 END_TM 등으로 FINISHED 여부를 판단해도 좋음.
                    game.setStatus("FINISHED");
                } else {
                    game.setStatus("SCHEDULED");
                }

                gameRepository.save(game);
                log.info("업데이트 완료 - gameId: {}", game.getGameId());
            } else {
                log.error("HTTP 오류 ({}): gameId {}", responseEntity.getStatusCodeValue(), game.getGameId());
            }
        } catch (Exception e) {
            log.error("요청 중 오류 발생: gameId {} - {}", game.getGameId(), e.getMessage(), e);
        }
    }

    private boolean isValidTimeString(String timeStr) {
        return timeStr != null && !timeStr.isBlank() && !timeStr.equalsIgnoreCase("null");
    }
}
