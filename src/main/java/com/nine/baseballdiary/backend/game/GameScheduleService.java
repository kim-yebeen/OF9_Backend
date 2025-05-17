package com.nine.baseballdiary.backend.game;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

@Component
public class GameScheduleService {

    private final GameService gameService;
    private static final DateTimeFormatter DB_DATE  = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");
    private static final Pattern SCORE_PATTERN = Pattern.compile("([가-힣A-Z]+)\\s*(\\d+)?");
    private static final Logger logger = Logger.getLogger(GameScheduleService.class.getName());

    public GameScheduleService(GameService gameService) {
        this.gameService = gameService;
    }

    //매주 월요일 새벽 1시 전체 크롤링
    @Scheduled(cron = "0 0 3 * * 1")
    public void weeklyInitialCrawl() {
        crawlSchedule(true);
    }
    //매일 아침 11시 해당 월 크롤링
    @Scheduled(cron = "0 0 18 * * *")
    public void dailyUpdate11() {
        crawlSchedule(false);
    }
    //매일 21시 해당 월 크롤링
    @Scheduled(cron = "0 0 21 * * *")
    public void dailyUpdate21() {
        crawlSchedule(false);
    }

    @Scheduled(cron = "0 0 22 * * *")
    public void dailyUpdate22() {
        crawlSchedule(false);
    }

    @Scheduled(cron = "0 0 23 * * *")
    public void dailyUpdate23() {
        crawlSchedule(false);
    }

    public void crawlSchedule(boolean fullCrawl) {
        WebDriver driver = null;
        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1920,1080");
            driver = new ChromeDriver(options);

            driver.get("https://www.koreabaseball.com/Schedule/Schedule.aspx");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            String year = driver.findElement(By.id("ddlYear")).getAttribute("value");
            String currentMonth = driver.findElement(By.id("ddlMonth")).getAttribute("value");
            int startMonth = fullCrawl ? 1 : Integer.parseInt(currentMonth);
            int endMonth   = fullCrawl ? 12 : Integer.parseInt(currentMonth);


            for (int m = startMonth; m <= endMonth; m++) {
                String monthVal = String.format("%02d", m);
                try {
                    // 월 선택
                    new Select(driver.findElement(By.id("ddlMonth"))).selectByValue(monthVal);

                    // 해당 월의 첫 번째 날짜 셀이 나타날 때까지 대기
                    try {
                        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                                By.cssSelector("#tblScheduleList tbody tr:first-child td.day"),
                                monthVal + "."
                        ));
                    } catch (TimeoutException | NoSuchElementException e) {
                        // 데이터가 없는 월은 스킵
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
                        String dbDateStr = year
                                + String.format("%02d", Integer.parseInt(dateParts[0]))
                                + String.format("%02d", Integer.parseInt(dateParts[1]));
                        LocalDate gameDate = LocalDate.parse(dbDateStr, DB_DATE);

                        String timeText = row.findElement(By.cssSelector("td.time")).getText().trim();
                        LocalTime startTime = timeText.isBlank() ? null : LocalTime.parse(timeText, TIME_FMT);

                        List<WebElement> tds = row.findElements(By.tagName("td"));
                        String stadium = tds.size() >= 8
                                ? tds.get(tds.size() - 2).getText().trim()
                                : "";

                        WebElement playCell = row.findElement(By.cssSelector("td.play"));
                        String playText = playCell.getText().trim(); // ex: "KT 3 vs 3 두산"

                        logger.info("원본 텍스트: " + playText);

                        String awayName = "", homeName = "";
                        int awayScore = 0, homeScore = 0;
                        String status = "SCHEDULED";

                        String[] vsParts = playText.split("\\s*vs\\s*");
                        if (vsParts.length == 2) {
                            String left = vsParts[0].trim();
                            String right = vsParts[1].trim();

                            logger.info("분리된 텍스트 - 왼쪽: " + left + ", 오른쪽: " + right);

                            // 원정팀(왼쪽) 처리
                            Pattern awayPattern = Pattern.compile("([가-힣A-Z]+)\\s*(\\d*).*");
                            Matcher awayMatcher = awayPattern.matcher(left);
                            if (awayMatcher.matches()) {
                                awayName = awayMatcher.group(1).trim();
                                String scoreStr = awayMatcher.group(2);
                                if (scoreStr != null && !scoreStr.isEmpty()) {
                                    try {
                                        awayScore = Integer.parseInt(scoreStr);
                                        status = "FINISHED";
                                    } catch (NumberFormatException e) {
                                        logger.warning("원정팀 점수 파싱 오류: " + scoreStr);
                                    }
                                }
                            } else {
                                awayName = left;
                            }

                            // 홈팀(오른쪽) 처리
                            Pattern homePattern = Pattern.compile("(\\d*)\\s*([가-힣A-Z]+).*");
                            Matcher homeMatcher = homePattern.matcher(right);
                            if (homeMatcher.matches()) {
                                String scoreStr = homeMatcher.group(1);
                                homeName = homeMatcher.group(2).trim();
                                if (scoreStr != null && !scoreStr.isEmpty()) {
                                    try {
                                        homeScore = Integer.parseInt(scoreStr);
                                        if ("SCHEDULED".equals(status)) {
                                            status = "FINISHED";
                                        }
                                    } catch (NumberFormatException e) {
                                        logger.warning("홈팀 점수 파싱 오류: " + scoreStr);
                                    }
                                }
                            } else {
                                homeName = right;
                            }

                            logger.info("파싱 결과 - 원정팀: " + awayName + ", 원정점수: " + awayScore +
                                    ", 홈팀: " + homeName + ", 홈점수: " + homeScore);
                        }

                        String rowText = row.getText();
                        if (rowText.contains("우천취소") || rowText.contains("경기취소")) {
                            status = "CANCELED";
                        }

                        // 정확한 팀 코드로 게임 ID 생성
                        String awayCode = getTeamCode(awayName);
                        String homeCode = getTeamCode(homeName);

                        // 홈팀과 원정팀 코드를 올바른 순서로 배치 (홈팀이 뒤에 오도록)
                        String matchKey = dbDateStr + homeCode + awayCode;

                        logger.info("생성된 매치키: " + matchKey + " (날짜: " + dbDateStr +
                                ", 홈팀코드: " + homeCode + ", 원정팀코드: " + awayCode + ")");

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

                        logger.info("생성된 게임 객체: " + game);

                        if (fullCrawl) {
                            gameService.saveOrUpdateSchedule(game);
                        } else if (gameService.existsById(gameId)) {
                            gameService.updateResult(game);
                        } else {
                            gameService.saveGame(game);
                        }
                    }
                    System.out.println(monthVal + "월 크롤링 완료");
                } catch (Exception e) {
                    System.err.println(monthVal + "월 처리 오류: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("크롤링 중 오류: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) driver.quit();
        }
        System.out.println("크롤링 작업 완료");
    }

    private static String getTeamCode(String name) {
        // 팀명에서 불필요한 숫자나 공백 제거
        String cleanName = name.replaceAll("\\d+", "").trim();

        return switch (cleanName) {
            case "두산" -> "OB";
            case "NC"   -> "NC";
            case "KT"   -> "KT";
            case "KIA"  -> "HT";
            case "한화" -> "HH";
            case "LG"   -> "LG";
            case "키움" -> "WO";
            case "삼성" -> "SS";
            case "롯데" -> "LT";
            case "SSG"  -> "SK";
            default     -> {
                Logger.getLogger(GameScheduleService.class.getName())
                        .warning("알 수 없는 팀명: " + name + ", 정리된 팀명: " + cleanName);
                yield "XX";
            }
        };
    }
}