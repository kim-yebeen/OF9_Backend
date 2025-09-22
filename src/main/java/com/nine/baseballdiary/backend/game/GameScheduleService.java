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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GameScheduleService {

    private final GameService gameService;
    private static final DateTimeFormatter DB_DATE  = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");
    private static final Logger logger = Logger.getLogger(GameScheduleService.class.getName());

    public GameScheduleService(GameService gameService) {
        this.gameService = gameService;
    }

    //@Scheduled(cron = "0 0 11 * * *", zone = "Asia/Seoul")
    public void dailyUpdate11() {
        logger.info("일일 업데이트 시작 (오전 11시) - " + LocalDate.now());
        crawlSchedule(false);
    }

    //@Scheduled(cron = "0 0 21 * * *", zone = "Asia/Seoul")
    public void dailyUpdate21() {
        logger.info("일일 업데이트 시작 (오후 21시) - " + LocalDate.now());
        crawlSchedule(false);
    }

    //@Scheduled(cron = "0 0 22 * * *", zone = "Asia/Seoul")
    public void dailyUpdate22() {
        logger.info("일일 업데이트 시작 (오후 22시) - " + LocalDate.now());
        crawlSchedule(false);
    }

    @Scheduled(cron = "0 03 03 * * *", zone = "Asia/Seoul")
    public void dailyUpdate23() {
        logger.info("일일 업데이트 시작 (오후 23시) - " + LocalDate.now());
        crawlSchedule(false);
    }

    public void crawlSchedule(boolean fullCrawl) {
        WebDriver driver = null;
        try {
            logger.info("크롤링 시작 - fullCrawl: " + fullCrawl);

            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--window-size=1920,1080");
            options.addArguments("--disable-web-security", "--disable-features=VizDisplayCompositor", "--disable-extensions", "--disable-plugins");
            options.addArguments("--disable-images", "--disable-dev-tools", "--disable-logging", "--log-level=3", "--silent");
            options.addArguments("--remote-debugging-port=0", "--disable-blink-features=AutomationControlled");
            options.addArguments("--memory-pressure-off", "--max_old_space_size=512", "--aggressive-cache-discard");
            options.addArguments("--disable-background-timer-throttling", "--disable-renderer-backgrounding", "--disable-backgrounding-occluded-windows");
            options.addArguments("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);

            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(300));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));

            boolean pageLoaded = false;
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    driver.get("https://www.koreabaseball.com/Schedule/Schedule.aspx");
                    logger.info("페이지 로드 성공 (시도 " + attempt + "/3)");
                    pageLoaded = true;
                    break;
                } catch (TimeoutException e) {
                    logger.warning("페이지 로드 타임아웃 (시도 " + attempt + "/3): " + e.getMessage());
                    if (attempt == 3) throw new RuntimeException("페이지 로드 3회 실패", e);
                    Thread.sleep(5000);
                }
            }
            if (!pageLoaded) throw new RuntimeException("페이지 로드 최종 실패");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(180));

            String year = driver.findElement(By.id("ddlYear")).getAttribute("value");
            String currentMonth = driver.findElement(By.id("ddlMonth")).getAttribute("value");
            int startMonth = fullCrawl ? 1 : Integer.parseInt(currentMonth);
            int endMonth   = fullCrawl ? 12 : Integer.parseInt(currentMonth);

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
                    } catch (TimeoutException | NoSuchElementException e) {
                        logger.info(monthVal + "월에 스케줄 데이터가 없어 건너뜁니다.");
                        continue;
                    }

                    List<WebElement> rows = driver.findElements(By.cssSelector("#tblScheduleList tbody tr"));
                    String currentDayRaw = "";
                    Map<String, Integer> doubleHeaderCounter = new HashMap<>();

                    for (WebElement row : rows) {
                        List<WebElement> days = row.findElements(By.cssSelector("td.day"));
                        if (!days.isEmpty()) {
                            currentDayRaw = days.get(0).getText().split("\\(")[0].trim();
                        }
                        if (currentDayRaw.isBlank() || !currentDayRaw.matches(".*\\d+.*")) continue;

                        String[] dateParts = currentDayRaw.split("\\.");
                        String dbDateStr = year + String.format("%02d", Integer.parseInt(dateParts[0])) + String.format("%02d", Integer.parseInt(dateParts[1]));
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

                        boolean isCanceledByCss = !row.findElements(By.cssSelector("td.cancel, span.cancel")).isEmpty();

                        if (isCanceledByCss || rowText.contains("우천취소") || rowText.contains("경기취소") || rowText.contains("기타")) {
                            status = "CANCELED";
                        } else {
                            String[] vsParts = playText.split("\\s*vs\\s*");
                            if (vsParts.length == 2) {
                                String left = vsParts[0].trim();
                                String right = vsParts[1].trim();
                                boolean hasScore = false;

                                Pattern awayPattern = Pattern.compile("([가-힣A-Z]+)\\s*(\\d*)");
                                Matcher awayMatcher = awayPattern.matcher(left);
                                if (awayMatcher.find()) {
                                    awayName = awayMatcher.group(1).trim();
                                    String scoreStr = awayMatcher.group(2);
                                    if (scoreStr != null && !scoreStr.isEmpty()) {
                                        awayScore = Integer.parseInt(scoreStr);
                                        hasScore = true;
                                    }
                                } else {
                                    awayName = left;
                                }

                                Pattern homePattern = Pattern.compile("(\\d+)\\s*([가-힣A-Z]+)");
                                Matcher homeMatcher = homePattern.matcher(right);
                                if (homeMatcher.find()) {
                                    homeScore = Integer.parseInt(homeMatcher.group(1));
                                    homeName = homeMatcher.group(2).trim();
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
                    }
                    logger.info(monthVal + "월 크롤링 완료 - 처리된 게임 수: " + rows.size());
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

    private String determineGameStatus(LocalDate gameDate, LocalDate today, boolean hasScore, String rowText, boolean hasHighlight) {
        if (rowText.contains("우천취소") || rowText.contains("경기취소") || rowText.contains("기타")) {
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