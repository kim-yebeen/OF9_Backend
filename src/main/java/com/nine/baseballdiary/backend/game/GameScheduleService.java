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
    @Scheduled(cron = "0 59 15 * * *") //@Scheduled(cron = "0 23 16 * ?")
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

            LocalDate today = LocalDate.now();

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

                        // 하이라이트 링크 확인
                        boolean hasHighlight = false;
                        try {
                            List<WebElement> highlightLinks = row.findElements(By.cssSelector("a[href*='highlight'], a[href*='Highlight']"));
                            hasHighlight = !highlightLinks.isEmpty();
                            if (hasHighlight) {
                                logger.info("하이라이트 링크 발견: 경기 종료로 판단");
                            }
                        } catch (Exception e) {
                            // 하이라이트 링크 확인 실패시 무시
                        }

                        logger.info("원본 텍스트: " + playText + ", 하이라이트 존재: " + hasHighlight);

                        String awayName = "", homeName = "";
                        int awayScore = 0, homeScore = 0;
                        String status = "SCHEDULED";

                        // 취소 상태 먼저 확인
                        String rowText = row.getText();
                        if (rowText.contains("우천취소") || rowText.contains("경기취소") || rowText.contains("기타")) {
                            status = "CANCELED";
                        } else {
                            String[] vsParts = playText.split("\\s*vs\\s*");
                            if (vsParts.length == 2) {
                                String left = vsParts[0].trim();
                                String right = vsParts[1].trim();

                                logger.info("분리된 텍스트 - 왼쪽: " + left + ", 오른쪽: " + right);

                                boolean hasScore = false;

                                // 원정팀(왼쪽) 처리
                                Pattern awayPattern = Pattern.compile("([가-힣A-Z]+)\\s*(\\d*).*");
                                Matcher awayMatcher = awayPattern.matcher(left);
                                if (awayMatcher.matches()) {
                                    awayName = awayMatcher.group(1).trim();
                                    String scoreStr = awayMatcher.group(2);
                                    if (scoreStr != null && !scoreStr.isEmpty()) {
                                        try {
                                            awayScore = Integer.parseInt(scoreStr);
                                            hasScore = true;
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
                                            hasScore = true;
                                        } catch (NumberFormatException e) {
                                            logger.warning("홈팀 점수 파싱 오류: " + scoreStr);
                                        }
                                    }
                                } else {
                                    homeName = right;
                                }

                                // 상태 결정 로직 개선
                                status = determineGameStatus(gameDate, today, hasScore, awayScore, homeScore, rowText, hasHighlight);

                                logger.info("파싱 결과 - 원정팀: " + awayName + ", 원정점수: " + awayScore +
                                        ", 홈팀: " + homeName + ", 홈점수: " + homeScore + ", 상태: " + status);
                            }
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
                            // 기존 게임의 상태를 확인하여 업데이트 여부 결정
                            Game existingGame = gameService.findById(gameId).orElse(null);
                            if (existingGame != null && shouldUpdateGame(existingGame, game)) {
                                gameService.updateResult(game);
                            }
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

    /**
     * 게임 상태를 결정하는 로직
     */
    private String determineGameStatus(LocalDate gameDate, LocalDate today, boolean hasScore,
                                       int awayScore, int homeScore, String rowText, boolean hasHighlight) {

        // 취소 상태 확인
        if (rowText.contains("우천취소") || rowText.contains("경기취소") || rowText.contains("기타")) {
            return "CANCELED";
        }

        // 하이라이트가 있으면 경기 완료로 처리
        if (hasHighlight && hasScore) {
            logger.info("하이라이트 존재 + 점수 있음 -> FINISHED");
            return "FINISHED";
        }

        // 경기 날짜가 오늘보다 이전인 경우
        if (gameDate.isBefore(today)) {
            if (hasScore) {
                // 과거 경기이고 점수가 있으면 완료
                return "FINISHED";
            } else {
                // 과거 경기인데 점수가 없으면 취소되었을 가능성
                return "CANCELED";
            }
        }

        // 경기 날짜가 오늘인 경우
        if (gameDate.equals(today)) {
            if (hasScore && (awayScore > 0 || homeScore > 0)) {
                // 오늘 경기이고 0이 아닌 점수가 있으면 진행중
                // 하이라이트가 없으면 아직 진행중일 가능성
                return "IN_PROGRESS";
            } else if (hasScore && awayScore == 0 && homeScore == 0) {
                // 오늘 경기이고 0:0이면 경기 시작 전 또는 진행중
                return "SCHEDULED";
            } else {
                // 점수 정보가 없으면 예정
                return "SCHEDULED";
            }
        }

        // 미래 경기
        return "SCHEDULED";
    }

    /**
     * 기존 게임과 새로운 게임 정보를 비교하여 업데이트 여부 결정
     */
    private boolean shouldUpdateGame(Game existingGame, Game newGame) {
        // 이미 완료된 게임은 업데이트하지 않음 (단, 당일 경기는 예외)
        if ("FINISHED".equals(existingGame.getStatus()) &&
                !newGame.getDate().equals(LocalDate.now())) {
            return false;
        }

        // 점수나 상태가 변경된 경우 업데이트
        return existingGame.getAwayScore() != newGame.getAwayScore() ||
                existingGame.getHomeScore() != newGame.getHomeScore() ||
                !existingGame.getStatus().equals(newGame.getStatus());
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