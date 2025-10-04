package com.nine.baseballdiary.backend.game;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GameScheduleService {

    private final GameService gameService;
    private static final DateTimeFormatter DB_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");
    private static final Logger logger = Logger.getLogger(GameScheduleService.class.getName());

    // 정규식 패턴을 미리 컴파일
    private static final Pattern AWAY_PATTERN = Pattern.compile("([가-힣A-Z]+)\\s*(\\d+)?");
    private static final Pattern HOME_PATTERN1 = Pattern.compile("(\\d+)\\s*([가-힣A-Z]+)");
    private static final Pattern HOME_PATTERN2 = Pattern.compile("([가-힣A-Z]+)\\s*(\\d+)");

    public GameScheduleService(GameService gameService) {
        this.gameService = gameService;
    }

    @Scheduled(cron = "0 3 14 * * *", zone = "Asia/Seoul")
    public void dailyUpdate11() {
        logger.info("일일 업데이트 시작 (오전 11시) - " + LocalDate.now());
        crawlSchedule(true);
    }

    public void crawlSchedule(boolean fullCrawl) {
        WebDriver driver = null;
        try {
            logger.info("크롤링 시작 - fullCrawl: " + fullCrawl);

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.setBinary("/usr/bin/google-chrome");
            options.addArguments("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36");
            options.setPageLoadStrategy(PageLoadStrategy.NORMAL); // EAGER에서 NORMAL로 변경

            driver = new ChromeDriver(options);

            // ⚡ 대기 시간 대폭 축소
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60)); // 300초 -> 60초
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3)); // 30초 -> 3초
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));

            // 페이지 로드
            boolean pageLoaded = false;
            for (int attempt = 1; attempt <= 2; attempt++) { // 3회 -> 2회
                try {
                    driver.get("https://www.koreabaseball.com/Schedule/Schedule.aspx");
                    logger.info("페이지 로드 성공 (시도 " + attempt + "/2)");
                    pageLoaded = true;
                    break;
                } catch (TimeoutException e) {
                    logger.warning("페이지 로드 타임아웃 (시도 " + attempt + "/2)");
                    if (attempt == 2) throw new RuntimeException("페이지 로드 실패", e);
                    Thread.sleep(2000);
                }
            }

            if (!pageLoaded) throw new RuntimeException("페이지 로드 최종 실패");

            // ⚡ 대기 시간 축소
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20)); // 180초 -> 20초

            String year = driver.findElement(By.id("ddlYear")).getAttribute("value");
            String currentMonth = driver.findElement(By.id("ddlMonth")).getAttribute("value");
            int startMonth = fullCrawl ? 1 : Integer.parseInt(currentMonth);
            int endMonth = fullCrawl ? 12 : Integer.parseInt(currentMonth);

            LocalDate today = LocalDate.now();
            logger.info("크롤링 범위: " + startMonth + "월 ~ " + endMonth + "월");

            // ⚡ 배치 저장을 위한 리스트
            List<Game> gamesToSave = new ArrayList<>();

            for (int m = startMonth; m <= endMonth; m++) {
                String monthVal = String.format("%02d", m);
                try {
                    logger.info(monthVal + "월 크롤링 시작");
                    new Select(driver.findElement(By.id("ddlMonth"))).selectByValue(monthVal);

                    try {
                        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                                By.cssSelector("#tblScheduleList tbody tr:first-child td.day"),
                                monthVal + "."
                        ));
                    } catch (TimeoutException | NoSuchElementException e) {
                        logger.info(monthVal + "월에 스케줄 데이터가 없어 건너뜁니다.");
                        continue;
                    }

                    // ⚡ 한 번만 조회
                    List<WebElement> rows = driver.findElements(By.cssSelector("#tblScheduleList tbody tr"));
                    String currentDayRaw = "";
                    Map<String, Integer> doubleHeaderCounter = new HashMap<>();

                    for (WebElement row : rows) {
                        try {
                            Game game = parseGameRow(row, year, currentDayRaw, doubleHeaderCounter, today);
                            if (game != null) {
                                currentDayRaw = game.getDate().format(DateTimeFormatter.ofPattern("MM.dd"));

                                if (fullCrawl) {
                                    gamesToSave.add(game);
                                } else {
                                    Game existingGame = gameService.findById(game.getGameId()).orElse(null);
                                    if (existingGame == null) {
                                        gamesToSave.add(game);
                                    } else if (shouldUpdateGame(existingGame, game)) {
                                        logger.info("게임 업데이트: " + game.getGameId());
                                        gameService.updateResult(game);
                                    }
                                }
                            }
                        } catch (Exception rowException) {
                            logger.warning("행 처리 중 오류: " + rowException.getMessage());
                        }
                    }

                    // ⚡ 배치 저장 (월별로 한 번에 저장)
                    if (!gamesToSave.isEmpty()) {
                        saveBatch(gamesToSave, fullCrawl);
                        logger.info(monthVal + "월 저장 완료 - 게임 수: " + gamesToSave.size());
                        gamesToSave.clear();
                    }

                    logger.info(monthVal + "월 크롤링 완료");
                    // Thread.sleep 제거 - 불필요

                } catch (Exception e) {
                    logger.severe(monthVal + "월 처리 중 오류: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            logger.severe("크롤링 중 심각한 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                    logger.info("WebDriver 정상 종료");
                } catch (Exception e) {
                    logger.warning("WebDriver 종료 중 오류: " + e.getMessage());
                }
            }
        }
        logger.info("크롤링 작업 완료");
    }

    // ⚡ 경기 파싱 메서드 분리
    private Game parseGameRow(WebElement row, String year, String currentDayRaw,
                              Map<String, Integer> doubleHeaderCounter, LocalDate today) {
        try {
            // 날짜 파싱
            List<WebElement> days = row.findElements(By.cssSelector("td.day"));
            if (!days.isEmpty()) {
                currentDayRaw = days.get(0).getText().split("\\(")[0].trim();
            }
            if (currentDayRaw.isBlank() || !currentDayRaw.matches(".*\\d+.*")) {
                return null;
            }

            String[] dateParts = currentDayRaw.split("\\.");
            String dbDateStr = year + String.format("%02d", Integer.parseInt(dateParts[0]))
                    + String.format("%02d", Integer.parseInt(dateParts[1]));
            LocalDate gameDate = LocalDate.parse(dbDateStr, DB_DATE);

            String timeText = row.findElement(By.cssSelector("td.time")).getText().trim();
            LocalTime startTime = timeText.isBlank() ? null : LocalTime.parse(timeText, TIME_FMT);

            List<WebElement> tds = row.findElements(By.tagName("td"));
            String stadium = tds.size() >= 8 ? tds.get(tds.size() - 2).getText().trim() : "";
            WebElement playCell = row.findElement(By.cssSelector("td.play"));
            String playText = playCell.getText().trim();
            boolean hasHighlight = !row.findElements(By.cssSelector("a[href*='highlight'], a[href*='Highlight']")).isEmpty();

            String awayName = "", homeName = "";
            int awayScore = 0, homeScore = 0;
            String status = "SCHEDULED";
            String rowText = row.getText();

            // ⚡ 취소 감지 개선
            boolean isCanceled = isCanceledGame(row, rowText, playText);

            if (isCanceled) {
                status = "CANCELED";
                // ✅ 취소된 경기도 팀명 파싱
                String[] vsParts = playText.split("\\s*vs\\s*");
                if (vsParts.length == 2) {
                    awayName = vsParts[0].replaceAll("\\d", "").trim();
                    homeName = vsParts[1].replaceAll("\\d", "").trim();
                }
                logger.info("취소된 경기: " + awayName + " vs " + homeName);
            } else {
                String[] vsParts = playText.split("\\s*vs\\s*");
                if (vsParts.length == 2) {
                    String left = vsParts[0].trim();
                    String right = vsParts[1].trim();
                    boolean hasScore = false;

                    // Away 팀 파싱
                    Matcher awayMatcher = AWAY_PATTERN.matcher(left);
                    if (awayMatcher.find()) {
                        awayName = awayMatcher.group(1).trim();
                        String scoreStr = awayMatcher.group(2);
                        if (scoreStr != null && !scoreStr.isEmpty()) {
                            awayScore = Integer.parseInt(scoreStr);
                            hasScore = true;
                        }
                    } else {
                        awayName = left.replaceAll("\\d", "").trim();
                    }

                    // Home 팀 파싱
                    Matcher homeMatcher1 = HOME_PATTERN1.matcher(right);
                    Matcher homeMatcher2 = HOME_PATTERN2.matcher(right);

                    if (homeMatcher1.find()) {
                        homeScore = Integer.parseInt(homeMatcher1.group(1));
                        homeName = homeMatcher1.group(2).trim();
                        hasScore = true;
                    } else if (homeMatcher2.find()) {
                        homeName = homeMatcher2.group(1).trim();
                        homeScore = Integer.parseInt(homeMatcher2.group(2));
                        hasScore = true;
                    } else {
                        homeName = right.replaceAll("\\d", "").trim();
                    }

                    status = determineGameStatus(gameDate, today, hasScore, rowText, hasHighlight);
                }
            }

            // ✅ 팀 코드 검증 (취소된 경기도 저장)
            String awayCode = getTeamCode(awayName);
            String homeCode = getTeamCode(homeName);
            if ("XX".equals(awayCode) || "XX".equals(homeCode)) {
                logger.warning("알 수 없는 팀 코드: " + awayName + " -> " + awayCode +
                        ", " + homeName + " -> " + homeCode + " | Status: " + status);
                return null; // ⚠️ 이 부분이 문제! 취소된 경기도 팀명이 비어있으면 null 반환
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

            return game;

        } catch (Exception e) {
            logger.warning("게임 파싱 실패: " + e.getMessage());
            return null;
        }
    }

    // ⚡ 취소 감지 개선
    private boolean isCanceledGame(WebElement row, String rowText, String playText) {
        // CSS 클래스 체크
        boolean hasCancelClass = !row.findElements(By.cssSelector("td.cancel, span.cancel, .cancel")).isEmpty();

        // 텍스트 체크
        boolean hasCancelText = rowText.contains("우천취소") ||
                rowText.contains("경기취소") ||
                rowText.contains("기타") ||
                rowText.contains("취소") ||
                rowText.contains("연기") ||
                playText.contains("취소") ||
                playText.contains("연기");

        return hasCancelClass || hasCancelText;
    }

    // ⚡ 배치 저장
    private void saveBatch(List<Game> games, boolean fullCrawl) {
        for (Game game : games) {
            if (fullCrawl) {
                gameService.saveOrUpdateSchedule(game);
            } else {
                gameService.saveGame(game);
            }
        }
    }

    private String determineGameStatus(LocalDate gameDate, LocalDate today, boolean hasScore,
                                       String rowText, boolean hasHighlight) {
        if (rowText.contains("우천취소") || rowText.contains("경기취소") ||
                rowText.contains("기타") || rowText.contains("취소") || rowText.contains("연기")) {
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