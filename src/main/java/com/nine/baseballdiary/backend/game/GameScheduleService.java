package com.nine.baseballdiary.backend.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GameScheduleService {

    private final GameService gameService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DB_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");
    private static final Logger logger = Logger.getLogger(GameScheduleService.class.getName());

    public GameScheduleService(GameService gameService, RestTemplate restTemplate) {
        this.gameService = gameService;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }
    @Scheduled(cron = "0 13 11 * * *", zone = "Asia/Seoul")
    public void performDailyCrawl() {
        logger.info("정기 스케줄러 실행: 전체 크롤링을 시작합니다.");
        // 2. 이 안에서 기존 메소드를 원하는 파라미터(true)로 호출합니다.
        crawlSchedule(true);
    }

    // @Scheduled 어노테이션은 그대로 사용하시면 됩니다.
    //@Scheduled(cron = "0 55 10 * * *", zone = "Asia/Seoul")
    public void crawlSchedule(boolean fullCrawl) {
        logger.info("API 기반 크롤링 시작 - fullCrawl: " + fullCrawl);
        String url = "https://www.koreabaseball.com/ws/Schedule.asmx/GetScheduleList";

        HttpHeaders headers = new HttpHeaders();
        // 폼 데이터 형식으로 Content-Type 설정
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        headers.set("X-Requested-With", "XMLHttpRequest");

        int currentYear = LocalDate.now().getYear();
        int startMonth = fullCrawl ? 3 : LocalDate.now().getMonthValue();
        int endMonth = fullCrawl ? 11 : LocalDate.now().getMonthValue();

        List<Game> allGames = new ArrayList<>();

        for (int m = startMonth; m <= endMonth; m++) {
            String month = String.format("%02d", m);
            logger.info(currentYear + "년 " + month + "월 데이터 요청...");

            try {
                // KBO API가 요구하는 폼 데이터 생성
                MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
                requestBody.add("leId", "1");
                requestBody.add("srIdList", "0,,9,6");
                requestBody.add("seasonId", String.valueOf(currentYear));
                requestBody.add("gameMonth", month);
                requestBody.add("teamId", "");

                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);
                String responseJson = restTemplate.postForObject(url, request, String.class);

                Map<String, Object> decodedResponse = objectMapper.readValue(responseJson, new TypeReference<>() {});
                List<Map<String, List<Map<String, String>>>> rowsData = (List<Map<String, List<Map<String, String>>>>) decodedResponse.get("rows");

                if (rowsData == null || rowsData.isEmpty()) {
                    logger.info(month + "월에 스케줄 데이터가 없습니다.");
                    continue;
                }

                allGames.addAll(parseGameData(rowsData, String.valueOf(currentYear)));
                Thread.sleep(200); // 서버 부하 방지를 위한 최소한의 대기

            } catch (Exception e) {
                logger.severe(month + "월 처리 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (!allGames.isEmpty()) {
            gameService.saveAllGames(allGames);
            logger.info("총 " + allGames.size() + "개의 게임 정보를 DB에 저장 완료!");
        }
        logger.info("API 기반 크롤링 작업 완료");
    }

    private List<Game> parseGameData(List<Map<String, List<Map<String, String>>>> rowsData, String year) {
        List<Game> monthlyGames = new ArrayList<>();
        String currentDayRaw = "";
        Map<String, Integer> doubleHeaderCounter = new HashMap<>();

        for (Map<String, List<Map<String, String>>> rowMap : rowsData) {
            try {
                List<Map<String, String>> row = rowMap.get("row");
                if (row.isEmpty()) continue;

                int timeIdx, playIdx, relayIdx, highlightIdx, stadiumIdx, remarksIdx;

                if ("day".equals(row.get(0).get("Class"))) {
                    currentDayRaw = row.get(0).get("Text").split("\\(")[0].trim();
                    timeIdx = 1; playIdx = 2; relayIdx = 3; highlightIdx = 4; stadiumIdx = 7; remarksIdx = 8;
                } else {
                    timeIdx = 0; playIdx = 1; relayIdx = 2; highlightIdx = 3; stadiumIdx = 6; remarksIdx = 7;
                }

                if (currentDayRaw.isBlank()) continue;

                String timeText = row.get(timeIdx).get("Text").replaceAll("<[^>]*>", "").trim();
                String playData = row.get(playIdx).get("Text"); // HTML 포함
                String playText = playData.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
                String stadium = row.get(stadiumIdx).get("Text").trim();
                String remarks = row.get(remarksIdx).get("Text").trim();
                String relayText = row.get(relayIdx).get("Text");

                String awayName = "", homeName = "";
                int awayScore = 0, homeScore = 0;
                String status = "SCHEDULED"; // 기본값

                // 1. 경기 상태 판단
                if (remarks.contains("취소")) {
                    status = "CANCELED";
                } else if (relayText.contains("btnReview") || relayText.contains("btnHighlight")) {
                    status = "FINISHED";
                }

                // 2. 팀 이름 파싱 (모든 상태 공통)
                String[] vsParts = playText.split("vs");
                if (vsParts.length == 2) {
                    Pattern teamNamePattern = Pattern.compile("([가-힣A-Z]+)");
                    Matcher awayMatcher = teamNamePattern.matcher(vsParts[0]);
                    if (awayMatcher.find()) awayName = awayMatcher.group(1).trim();

                    Matcher homeMatcher = teamNamePattern.matcher(vsParts[1]);
                    if (homeMatcher.find()) homeName = homeMatcher.group(1).trim();
                }

                // 3. 점수 파싱 (FINISHED 상태일 때만)
                if ("FINISHED".equals(status)) {
                    Pattern awayScorePattern = Pattern.compile("([가-힣A-Z]+)\\s*(\\d+)");
                    Matcher awayScoreMatcher = awayScorePattern.matcher(playText);
                    if(awayScoreMatcher.find()) {
                        awayScore = Integer.parseInt(awayScoreMatcher.group(2));
                    }

                    Pattern homeScorePattern = Pattern.compile("(\\d+)\\s*([가-힣A-Z]+)");
                    Matcher homeScoreMatcher = homeScorePattern.matcher(playText);
                    if(homeScoreMatcher.find()) {
                        homeScore = Integer.parseInt(homeScoreMatcher.group(1));
                    }
                }

                String awayCode = getTeamCode(awayName);
                String homeCode = getTeamCode(homeName);
                if ("XX".equals(awayCode) || "XX".equals(homeCode)) continue;

                String[] dateParts = currentDayRaw.split("\\.");
                String dbDateStr = year + String.format("%02d", Integer.parseInt(dateParts[0])) + String.format("%02d", Integer.parseInt(dateParts[1]));

                String matchKey = dbDateStr + homeCode + awayCode;
                int gameNumber = doubleHeaderCounter.getOrDefault(matchKey, 0);
                doubleHeaderCounter.put(matchKey, gameNumber + 1);
                String gameId = matchKey + gameNumber;

                Game game = new Game();
                game.setGameId(gameId);
                game.setDate(LocalDate.parse(dbDateStr, DB_DATE));
                game.setTime(timeText.isBlank() ? null : LocalTime.parse(timeText, TIME_FMT));
                game.setStadium(stadium);
                game.setAwayTeam(awayName);
                game.setHomeTeam(homeName);
                game.setAwayScore(awayScore);
                game.setHomeScore(homeScore);
                game.setStatus(status);
                monthlyGames.add(game);

            } catch (Exception e) {
                logger.warning("개별 경기 데이터 파싱 중 오류: " + e.getMessage());
            }
        }
        return monthlyGames;
    }

    private static String getTeamCode(String name) {
        if (name == null || name.isEmpty()) return "XX";
        return switch (name) {
            case "두산" -> "OB"; case "NC" -> "NC"; case "KT" -> "KT";
            case "KIA" -> "HT"; case "한화" -> "HH"; case "LG" -> "LG";
            case "키움" -> "WO"; case "삼성" -> "SS"; case "롯데" -> "LT";
            case "SSG" -> "SK"; default -> "XX";
        };
    }
}