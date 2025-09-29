package com.nine.baseballdiary.backend.game;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
@Component
public class GameScheduleService {

    private final GameService gameService;
    private static final DateTimeFormatter DB_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");
    private static final Logger logger = Logger.getLogger(GameScheduleService.class.getName());

    public GameScheduleService(GameService gameService) {
        this.gameService = gameService;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void dailyUpdate() {
        logger.info("일일 업데이트 시작 - " + LocalDate.now());
        crawlSchedule(false);
    }

    public void crawlSchedule(boolean fullCrawl) {
        try {
            logger.info("크롤링 시작 - fullCrawl: " + fullCrawl);

            String baseUrl = "https://www.koreabaseball.com/Schedule/Schedule.aspx";
            int currentMonth = LocalDate.now().getMonthValue();
            int startMonth = fullCrawl ? 3 : currentMonth;
            int endMonth = fullCrawl ? 10 : currentMonth;
            String year = String.valueOf(LocalDate.now().getYear());
            LocalDate today = LocalDate.now();

            logger.info("크롤링 범위: " + startMonth + "월 ~ " + endMonth + "월");

            for (int m = startMonth; m <= endMonth; m++) {
                String monthStr = String.format("%02d", m);
                logger.info(monthStr + "월 크롤링 시작");

                String monthUrl = baseUrl + "?year=" + year + "&month=" + monthStr;

                try {
                    Document doc = Jsoup.connect(monthUrl)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .timeout(15000)
                            .get();

                    Elements rows = doc.select("#tblScheduleList tbody tr");
                    if (rows.isEmpty()) {
                        logger.info(monthStr + "월에 스케줄 데이터가 없어 건너뜁니다.");
                        continue;
                    }

                    String currentDayRaw = "";
                    Map<String, Integer> doubleHeaderCounter = new HashMap<>();

                    for (Element row : rows) {
                        try {
                            Elements days = row.select("td.day");
                            if (!days.isEmpty()) {
                                currentDayRaw = days.get(0).text().split("\\(")[0].trim();
                            }

                            if (currentDayRaw.isBlank() || !currentDayRaw.matches(".*\\d+.*")) {
                                continue;
                            }

                            String[] dateParts = currentDayRaw.split("\\.");
                            String dbDateStr = year + String.format("%02d", Integer.parseInt(dateParts[0]))
                                    + String.format("%02d", Integer.parseInt(dateParts[1]));
                            LocalDate gameDate = LocalDate.parse(dbDateStr, DB_DATE);

                            String timeText = row.select("td.time").text().trim();
                            LocalTime startTime = timeText.isBlank() ? null : LocalTime.parse(timeText, TIME_FMT);

                            Elements tds = row.select("td");
                            String stadium = tds.size() >= 8 ? tds.get(tds.size() - 2).text().trim() : "";
                            String playText = row.select("td.play").text().trim();
                            boolean hasHighlight = !row.select("a[href*=highlight], a[href*=Highlight]").isEmpty();
                            String rowText = row.text();

                            String awayName = "", homeName = "";
                            int awayScore = 0, homeScore = 0;
                            String status = "SCHEDULED";

                            boolean isCanceled = rowText.contains("우천취소") ||
                                    rowText.contains("경기취소") ||
                                    rowText.contains("취소") ||
                                    rowText.contains("연기") ||
                                    playText.contains("취소") ||
                                    playText.contains("연기");

                            if (isCanceled) {
                                status = "CANCELED";
                                logger.info("취소된 경기 감지: " + playText);
                            } else {
                                String[] vsParts = playText.split("\\s*vs\\s*");
                                if (vsParts.length == 2) {
                                    String left = vsParts[0].trim();
                                    String right = vsParts[1].trim();
                                    boolean hasScore = false;

                                    String[] leftParts = left.split("\\s+");
                                    if (leftParts.length >= 2 && leftParts[leftParts.length - 1].matches("\\d+")) {
                                        awayName = String.join(" ", java.util.Arrays.copyOf(leftParts, leftParts.length - 1));
                                        awayScore = Integer.parseInt(leftParts[leftParts.length - 1]);
                                        hasScore = true;
                                    } else {
                                        awayName = left.replaceAll("\\d", "").trim();
                                    }

                                    String[] rightParts = right.split("\\s+");
                                    if (rightParts.length >= 2 && rightParts[0].matches("\\d+")) {
                                        homeScore = Integer.parseInt(rightParts[0]);
                                        homeName = String.join(" ", java.util.Arrays.copyOfRange(rightParts, 1, rightParts.length));
                                        hasScore = true;
                                    } else if (rightParts.length >= 2 && rightParts[rightParts.length - 1].matches("\\d+")) {
                                        homeName = String.join(" ", java.util.Arrays.copyOf(rightParts, rightParts.length - 1));
                                        homeScore = Integer.parseInt(rightParts[rightParts.length - 1]);
                                        hasScore = true;
                                    } else {
                                        homeName = right.replaceAll("\\d", "").trim();
                                    }

                                    status = determineGameStatus(gameDate, today, hasScore, rowText, hasHighlight);
                                }
                            }

                            String awayCode = getTeamCode(awayName);
                            String homeCode = getTeamCode(homeName);

                            if ("XX".equals(awayCode) || "XX".equals(homeCode)) {
                                logger.warning("알 수 없는 팀 코드: " + awayName + " -> " + awayCode + ", " + homeName + " -> " + homeCode);
                                continue;
                            }

                            String matchKey = dbDateStr + homeCode + awayCode;
                            int gameNumber = doubleHeaderCounter.getOrDefault(matchKey, 0);
                            doubleHeaderCounter.put(matchKey, gameNumber + 1);
                            String gameId = matchKey + gameNumber;

                            Game game = new Game();
                            game.setGameId(gameId);
                            game.setDate(gameDate);
                            game.setTime(startTime);
                            game.setStadium(stadium);
                            game.setAwayTeam(awayName);
                            game.setHomeTeam(homeName);
                            game.setAwayScore(awayScore);
                            game.setHomeScore(homeScore);
                            game.setStatus(status);

                            if (fullCrawl) {
                                gameService.saveOrUpdateSchedule(game);
                            } else {
                                Game existingGame = gameService.findById(gameId).orElse(null);
                                if (existingGame == null) {
                                    gameService.saveGame(game);
                                } else if (shouldUpdateGame(existingGame, game)) {
                                    logger.info("게임 업데이트: " + gameId);
                                    gameService.updateResult(game);
                                }
                            }
                        } catch (Exception rowException) {
                            logger.warning("행 처리 중 오류: " + rowException.getMessage());
                            continue;
                        }
                    }

                    logger.info(monthStr + "월 크롤링 완료 - 처리된 게임 수: " + rows.size());
                    Thread.sleep(2000);

                } catch (Exception e) {
                    logger.severe(monthStr + "월 처리 중 오류: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            logger.severe("크롤링 중 심각한 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }

        logger.info("크롤링 작업 완료");
    }

    private String determineGameStatus(LocalDate gameDate, LocalDate today, boolean hasScore,
                                       String rowText, boolean hasHighlight) {
        if (rowText.contains("우천취소") ||
                rowText.contains("경기취소") ||
                rowText.contains("취소") ||
                rowText.contains("연기")) {
            return "CANCELED";
        }

        if (hasHighlight && hasScore) {
            return "FINISHED";
        }

        if (gameDate.isBefore(today)) {
            return hasScore ? "FINISHED" : "CANCELED";
        }

        if (gameDate.equals(today) && hasScore) {
            return "IN_PROGRESS";
        }

        return "SCHEDULED";
    }

    private boolean shouldUpdateGame(Game existingGame, Game newGame) {
        if ("FINISHED".equals(existingGame.getStatus()) && !newGame.getDate().equals(LocalDate.now())) {
            return false;
        }

        return existingGame.getAwayScore() != newGame.getAwayScore() ||
                existingGame.getHomeScore() != newGame.getHomeScore() ||
                !existingGame.getStatus().equals(newGame.getStatus());
    }

    private static String getTeamCode(String name) {
        if (name == null || name.isEmpty()) {
            return "XX";
        }

        String cleanName = name.replaceAll("\\d+", "").trim();
        return switch (cleanName) {
            case "두산" -> "OB";
            case "NC" -> "NC";
            case "KT" -> "KT";
            case "KIA" -> "HT";
            case "한화" -> "HH";
            case "LG" -> "LG";
            case "키움" -> "WO";
            case "삼성" -> "SS";
            case "롯데" -> "LT";
            case "SSG" -> "SK";
            default -> "XX";
        };
    }
}