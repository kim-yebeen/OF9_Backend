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

    // GitHub Actions 배포 후 크롤링 실행 (UTC 11:48)
    @Scheduled(cron = "0 25 21 * * *")
    public void dailyFullCrawl() {
        logger.info("전체 크롤링 시작 - " + LocalDate.now());
        crawlSchedule(true);
    }

    // 매일 오전 11시 해당 월 크롤링
    //@Scheduled(cron = "0 0 11 * * *")
    public void dailyUpdate11() {
        logger.info("일일 업데이트 시작 (11시) - " + LocalDate.now());
        crawlSchedule(false);
    }

    // 매일 21시 해당 월 크롤링
    //@Scheduled(cron = "0 0 21 * * *")
    public void dailyUpdate21() {
        logger.info("일일 업데이트 시작 (21시) - " + LocalDate.now());
        crawlSchedule(false);
    }

    // 매일 22시 해당 월 크롤링
    //@Scheduled(cron = "0 0 22 * * *")
    public void dailyUpdate22() {
        logger.info("일일 업데이트 시작 (22시) - " + LocalDate.now());
        crawlSchedule(false);
    }

    // 매일 23시 해당 월 크롤링
    //@Scheduled(cron = "0 0 23 * * *")
    public void dailyUpdate23() {
        logger.info("일일 업데이트 시작 (23시) - " + LocalDate.now());
        crawlSchedule(false);
    }

    // 수동 실행용 메서드 (테스트나 즉시 실행용)
    public void manualCrawl() {
        logger.info("수동 크롤링 실행 - " + LocalDate.now());
        crawlSchedule(true);
    }

    public void crawlSchedule(boolean fullCrawl) {
        WebDriver driver = null;
        try {
            logger.info("크롤링 시작 - fullCrawl: " + fullCrawl);

            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();

            // Docker 환경용 기본 옵션들
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");

            // 안정성 개선 옵션들
            options.addArguments("--disable-web-security");
            options.addArguments("--disable-features=VizDisplayCompositor");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-plugins");
            options.addArguments("--disable-images");
            options.addArguments("--disable-javascript");
            options.addArguments("--disable-dev-tools");
            options.addArguments("--disable-logging");
            options.addArguments("--log-level=3");
            options.addArguments("--silent");

            // CDP 경고 해결용
            options.addArguments("--remote-debugging-port=0");
            options.addArguments("--disable-blink-features=AutomationControlled");

            // 메모리 최적화
            options.addArguments("--memory-pressure-off");
            options.addArguments("--max_old_space_size=4096");

            // User Agent 설정
            options.addArguments("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            // 페이지 로드 전략 - 빠른 로드
            options.setPageLoadStrategy(PageLoadStrategy.NORMAL);

            driver = new ChromeDriver(options);

            // 타임아웃 설정 강화
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(120)); // 2분으로 증가
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            logger.info("페이지 로드 시작: https://www.koreabaseball.com/Schedule/Schedule.aspx");

            // 재시도 로직 추가
            boolean pageLoaded = false;
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    driver.get("https://www.koreabaseball.com/Schedule/Schedule.aspx");
                    logger.info("페이지 로드 성공 (시도 " + attempt + "/3)");
                    pageLoaded = true;
                    break;
                } catch (TimeoutException e) {
                    logger.warning("페이지 로드 타임아웃 (시도 " + attempt + "/3): " + e.getMessage());
                    if (attempt == 3) {
                        throw new RuntimeException("페이지 로드 3회 실패", e);
                    }
                    // 5초 대기 후 재시도
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            if (!pageLoaded) {
                throw new RuntimeException("페이지 로드 실패");
            }

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

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
                        logger.info(monthVal + "월에 스케줄 데이터가 없어 건너뜀");
                        continue;
                    }

                    List<WebElement> rows = driver.findElements(By.cssSelector("#tblScheduleList tbody tr"));
                    String currentDayRaw = "";
                    Map<String, Integer> doubleHeaderCounter = new HashMap<>();

                    int processedGames = 0;
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
                        } catch (Exception e) {
                            // 하이라이트 링크 확인 실패시 무시
                        }

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

                                // 상태 결정
                                status = determineGameStatus(gameDate, today, hasScore, awayScore, homeScore, rowText, hasHighlight);
                            }
                        }

                        // 정확한 팀 코드로 게임 ID 생성
                        String awayCode = getTeamCode(awayName);
                        String homeCode = getTeamCode(homeName);

                        // 홈팀과 원정팀 코드를 올바른 순서로 배치 (홈팀이 뒤에 오도록)
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

                        try {
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
                            processedGames++;
                        } catch (Exception e) {
                            logger.warning("게임 저장 실패: " + gameId + ", 오류: " + e.getMessage());
                        }
                    }
                    logger.info(monthVal + "월 크롤링 완료 - 처리된 게임 수: " + processedGames);
                } catch (Exception e) {
                    logger.severe(monthVal + "월 처리 오류: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            logger.severe("크롤링 중 오류: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                    logger.info("WebDriver 정상 종료");
                } catch (Exception e) {
                    logger.warning("WebDriver 종료 중 오류: " + e.getMessage());
                    // 강제 종료 시도
                    try {
                        driver.close();
                    } catch (Exception ex) {
                        logger.warning("WebDriver 강제 종료도 실패");
                    }
                }
            }
        }
        logger.info("크롤링 작업 완료");
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