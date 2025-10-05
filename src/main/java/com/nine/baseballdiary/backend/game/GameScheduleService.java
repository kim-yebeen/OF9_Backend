package com.nine.baseballdiary.backend.game;

import com.nine.baseballdiary.backend.game.Game;
import com.nine.baseballdiary.backend.game.GameService;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GameScheduleService {

    private final GameService gameService;
    private static final DateTimeFormatter DB_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");
    private static final Logger logger = LoggerFactory.getLogger(GameScheduleService.class);

    public GameScheduleService(GameService gameService) {
        this.gameService = gameService;
    }

    @Scheduled(cron = "0 30 12 * * *", zone = "Asia/Seoul") // 테스트를 위해 현재 시간보다 2~3분 뒤로 설정
    public void performDailyCrawl() {
        logger.info("정기 스케줄러 실행 (Selenium): 전체 크롤링을 시작합니다.");
        crawlSchedule(true);
    }

    public void crawlSchedule(boolean fullCrawl) {
        WebDriver driver = null;
        try {
            logger.info("Selenium 기반 크롤링 시작 - fullCrawl: " + fullCrawl);

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
            // EC2 환경에 설치된 Chrome 경로를 지정해야 할 수 있습니다.
            // options.setBinary("/usr/bin/google-chrome");
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);

            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(90));
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

            driver.get("https://www.koreabaseball.com/Schedule/Schedule.aspx");
            logger.info("페이지 로드 성공");

            String year = driver.findElement(By.id("ddlYear")).getAttribute("value");
            int startMonth = fullCrawl ? 3 : LocalDate.now().getMonthValue();
            int endMonth = fullCrawl ? 11 : LocalDate.now().getMonthValue();

            List<Game> allGamesToSave = new ArrayList<>();

            for (int m = startMonth; m <= endMonth; m++) {
                String monthVal = String.format("%02d", m);
                logger.info(monthVal + "월 크롤링 시작");

                try {
                    WebElement scheduleTable = driver.findElement(By.id("tblScheduleList"));
                    new Select(driver.findElement(By.id("ddlMonth"))).selectByValue(monthVal);

                    // 안정적인 대기 로직
                    wait.until(ExpectedConditions.stalenessOf(scheduleTable));
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("tblScheduleList")));

                    List<WebElement> rows = driver.findElements(By.cssSelector("#tblScheduleList tbody tr"));
                    if (rows.isEmpty() || rows.get(0).getText().contains("해당 경기가 없습니다.")) {
                        logger.info(monthVal + "월에 스케줄 데이터가 없어 건너뜁니다.");
                        continue;
                    }

                    String currentDayRaw = "";
                    Map<String, Integer> doubleHeaderCounter = new HashMap<>();

                    for (WebElement row : rows) {
                        try {
                            List<WebElement> days = row.findElements(By.cssSelector("td.day"));
                            if (!days.isEmpty()) {
                                currentDayRaw = days.get(0).getText().split("\\(")[0].trim();
                            }
                            if (currentDayRaw.isBlank()) continue;

                            String playText = row.findElement(By.cssSelector("td.play")).getText();
                            String timeText = row.findElement(By.cssSelector("td.time")).getText().trim();
                            List<WebElement> tds = row.findElements(By.tagName("td"));
                            String stadium = tds.get(tds.size() - 2).getText().trim();
                            String remarks = tds.get(tds.size() - 1).getText().trim();

                            String awayName = "", homeName = "";
                            int awayScore = 0, homeScore = 0;
                            String status = "SCHEDULED";

                            // 버그 수정: 경기 상태와 관계없이 팀 이름 먼저 파싱
                            String[] vsParts = playText.split("\\s*vs\\s*");
                            if (vsParts.length == 2) {
                                Pattern awayPattern = Pattern.compile("([가-힣A-Z]+)\\s*(\\d+)?");
                                Matcher awayMatcher = awayPattern.matcher(vsParts[0]);
                                if (awayMatcher.find()) {
                                    awayName = awayMatcher.group(1).trim();
                                }

                                Pattern homePattern = Pattern.compile("(\\d+)?\\s*([가-힣A-Z]+)");
                                Matcher homeMatcher = homePattern.matcher(vsParts[1]);
                                if (homeMatcher.find()) {
                                    homeName = homeMatcher.group(2).trim();
                                }
                            }

                            // 경기 상태 판단
                            if (remarks.contains("취소")) {
                                status = "CANCELED";
                            } else if (playText.matches(".*\\d+\\s*vs\\s*\\d+.*")) {
                                status = "FINISHED";
                                // FINISHED 상태일 때만 점수 파싱
                                Pattern awayScorePattern = Pattern.compile("([가-힣A-Z]+)\\s*(\\d+)");
                                Matcher awayScoreMatcher = awayScorePattern.matcher(vsParts[0]);
                                if (awayScoreMatcher.find()) awayScore = Integer.parseInt(awayScoreMatcher.group(2));

                                Pattern homeScorePattern = Pattern.compile("(\\d+)\\s*([가-힣A-Z]+)");
                                Matcher homeScoreMatcher = homeScorePattern.matcher(vsParts[1]);
                                if (homeScoreMatcher.find()) homeScore = Integer.parseInt(homeScoreMatcher.group(1));
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

                            // 속도 개선: DB에 바로 저장하지 않고 리스트에 추가
                            allGamesToSave.add(game);

                        } catch (Exception e) {
                            logger.warn("개별 경기 데이터 파싱 중 오류: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logger.error(monthVal + "월 처리 중 오류 발생", e);
                }
            }

            // 속도 개선: 모든 크롤링이 끝난 후 DB에 한 번에 저장
            if (!allGamesToSave.isEmpty()) {
                gameService.saveAllGames(allGamesToSave);
                logger.info("총 " + allGamesToSave.size() + "개의 게임 정보를 DB에 저장 완료!");
            }

        } finally {
            if (driver != null) {
                driver.quit();
            }
            logger.info("Selenium 기반 크롤링 작업 완료");
        }
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