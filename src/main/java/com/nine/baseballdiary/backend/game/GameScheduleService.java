package com.nine.baseballdiary.backend.game;

import jakarta.annotation.PostConstruct;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GameScheduleService {

    private final GameService gameService;
    private static final DateTimeFormatter DB_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");
    private static final Logger logger = Logger.getLogger(GameScheduleService.class.getName());


    public GameScheduleService(GameService gameService) {
        this.gameService = gameService;
    }


    @PostConstruct
    public void init() {
        logger.info("애플리케이션 시작 - 30초 후 크롤링 시작 예정");
        new Thread(() -> {
            try {
                Thread.sleep(30000); // 30초 대기
                logger.info("크롤링 시작");
                crawlSchedule(true);
            } catch (InterruptedException e) {
                logger.warning("크롤링 대기 중 인터럽트: " + e.getMessage());
            }
        }).start();
    }


    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void dailyUpdate() {
        logger.info("일일 업데이트 시작 - " + LocalDate.now());
        crawlSchedule(false);
    }

    public void crawlSchedule(boolean fullCrawl) {
        WebDriver driver = null;
        try {
            logger.info("크롤링 시작 - fullCrawl: " + fullCrawl);

            ChromeOptions options = new ChromeOptions();

            // 메모리 최적화 옵션
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--disable-software-rasterizer");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-background-networking");
            options.addArguments("--disable-default-apps");
            options.addArguments("--disable-sync");
            options.addArguments("--disable-translate");
            options.addArguments("--hide-scrollbars");
            options.addArguments("--metrics-recording-only");
            options.addArguments("--mute-audio");
            options.addArguments("--no-first-run");
            options.addArguments("--safebrowsing-disable-auto-update");
            options.addArguments("--disable-images"); // 이미지 로드 안함
            options.addArguments("--blink-settings=imagesEnabled=false");
            options.addArguments("--window-size=1280,720");
            options.addArguments("--single-process"); // 메모리 절약
            options.addArguments("--disable-logging");
            options.addArguments("--log-level=3");
            options.addArguments("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36");
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);

            // 추가 메모리 최적화
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("profile.managed_default_content_settings.images", 2);
            prefs.put("profile.default_content_setting_values.notifications", 2);
            options.setExperimentalOption("prefs", prefs);

            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            driver.get("https://www.koreabaseball.com/Schedule/Schedule.aspx");
            logger.info("페이지 로드 성공");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            String year = driver.findElement(By.id("ddlYear")).getAttribute("value");
            String currentMonth = driver.findElement(By.id("ddlMonth")).getAttribute("value");
            int startMonth = fullCrawl ? 3 : Integer.parseInt(currentMonth);
            int endMonth = fullCrawl ? 10 : Integer.parseInt(currentMonth);

            LocalDate today = LocalDate.now();
            logger.info("크롤링 범위: " + startMonth + "월 ~ " + endMonth + "월");

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
                    } catch (TimeoutException e) {
                        logger.info(monthVal + "월에 스케줄 데이터가 없어 건너뜁니다.");
                        continue;
                    }

                    List<WebElement> rows = driver.findElements(By.cssSelector("#tblScheduleList tbody tr"));
                    String currentDayRaw = "";
                    Map<String, Integer> doubleHeaderCounter = new HashMap<>();

                    for (WebElement row : rows) {
                        try {
                            List<WebElement> days = row.findElements(By.cssSelector("td.day"));
                            if (!days.isEmpty()) {
                                currentDayRaw = days.get(0).getText().split("\\(")[0].trim();
                            }
                            if (currentDayRaw.isBlank() || !currentDayRaw.matches(".*\\d+.*")) continue;

                            String[] dateParts = currentDayRaw.split("\\.");
                            if (dateParts.length < 2) continue;

                            String dbDateStr = year + String.format("%02d", Integer.parseInt(dateParts[0]))
                                    + String.format("%02d", Integer.parseInt(dateParts[1]));
                            LocalDate gameDate = LocalDate.parse(dbDateStr, DB_DATE);

                            String timeText = row.findElement(By.cssSelector("td.time")).getText().trim();
                            LocalTime startTime = null;
                            if (!timeText.isBlank()) {
                                try {
                                    startTime = LocalTime.parse(timeText, TIME_FMT);
                                } catch (Exception e) {
                                    logger.warning("시간 파싱 실패: " + timeText);
                                }
                            }

                            List<WebElement> tds = row.findElements(By.tagName("td"));
                            String stadium = tds.size() >= 8 ? tds.get(tds.size() - 2).getText().trim() : "";
                            WebElement playCell = row.findElement(By.cssSelector("td.play"));
                            String playText = playCell.getText().trim();
                            boolean hasHighlight = !row.findElements(By.cssSelector("a[href*='highlight']")).isEmpty();

                            String awayName = "", homeName = "";
                            int awayScore = 0, homeScore = 0;
                            String status = "SCHEDULED";
                            String rowText = row.getText();

                            boolean isCanceled = rowText.contains("우천취소") || rowText.contains("경기취소") ||
                                    rowText.contains("취소") || rowText.contains("연기");

                            if (isCanceled) {
                                status = "CANCELED";
                            } else {
                                String[] vsParts = playText.split("\\s*vs\\s*");
                                if (vsParts.length == 2) {
                                    String left = vsParts[0].trim();
                                    String right = vsParts[1].trim();
                                    boolean hasScore = false;

                                    Pattern awayPattern = Pattern.compile("([가-힣A-Z]+)\\s*(\\d+)?");
                                    Matcher awayMatcher = awayPattern.matcher(left);
                                    if (awayMatcher.find()) {
                                        awayName = awayMatcher.group(1).trim();
                                        String scoreStr = awayMatcher.group(2);
                                        if (scoreStr != null && !scoreStr.isEmpty()) {
                                            awayScore = Integer.parseInt(scoreStr);
                                            hasScore = true;
                                        }
                                    }

                                    Pattern homePattern1 = Pattern.compile("(\\d+)\\s*([가-힣A-Z]+)");
                                    Pattern homePattern2 = Pattern.compile("([가-힣A-Z]+)\\s*(\\d+)");

                                    Matcher homeMatcher1 = homePattern1.matcher(right);
                                    Matcher homeMatcher2 = homePattern2.matcher(right);

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

                            String awayCode = getTeamCode(awayName);
                            String homeCode = getTeamCode(homeName);
                            if ("XX".equals(awayCode) || "XX".equals(homeCode)) continue;

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
                                    gameService.updateResult(game);
                                }
                            }
                        } catch (Exception rowException) {
                            logger.warning("행 처리 중 오류: " + rowException.getMessage());
                        }
                    }
                    logger.info(monthVal + "월 크롤링 완료");

                    // 메모리 정리
                    System.gc();
                    Thread.sleep(2000);

                } catch (Exception e) {
                    logger.severe(monthVal + "월 처리 중 오류: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            logger.severe("크롤링 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    logger.warning("WebDriver 종료 중 오류: " + e.getMessage());
                }
            }
            // 최종 메모리 정리
            System.gc();
        }
        logger.info("크롤링 작업 완료");
    }

    private String determineGameStatus(LocalDate gameDate, LocalDate today, boolean hasScore,
                                       String rowText, boolean hasHighlight) {
        if (rowText.contains("취소") || rowText.contains("연기")) return "CANCELED";
        if (hasHighlight && hasScore) return "FINISHED";
        if (gameDate.isBefore(today)) return hasScore ? "FINISHED" : "CANCELED";
        if (gameDate.equals(today) && hasScore) return "IN_PROGRESS";
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
        if (name == null || name.isEmpty()) return "XX";
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