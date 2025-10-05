package com.nine.baseballdiary.backend.game;

import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PostConstruct;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList; // 변경점
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
    private static final DateTimeFormatter DB_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");
    private static final Logger logger = Logger.getLogger(GameScheduleService.class.getName());

    @Value("${initial.crawl.enabled:false}")
    private boolean isInitialCrawlEnabled;

    public GameScheduleService(GameService gameService) {
        this.gameService = gameService;
    }

    @PostConstruct
    public void init() {
        // application.properties 파일에 initial.crawl.enabled=true 로 되어 있을 때만 실행
        if (isInitialCrawlEnabled) {
            logger.info("### @PostConstruct: 초기 크롤링을 시작합니다. ###");
            // 별도의 스레드에서 크롤링 실행 (웹 요청을 막지 않기 위함)
            new Thread(() -> crawlSchedule(true)).start();
        }
    }


    @Scheduled(cron = "0 33 9 * * *", zone = "Asia/Seoul")
    public void dailyUpdate11() {
        logger.info("일일 업데이트 시작 (오전 11시) - " + LocalDate.now());
        crawlSchedule(true);
    }

    public void crawlSchedule(boolean fullCrawl) {
        WebDriver driver = null;
        try {
            logger.info("크롤링 시작 - fullCrawl: " + fullCrawl);

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
            options.setBinary("/usr/bin/google-chrome");
            options.addArguments("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);

            driver = new ChromeDriver(options);
            // 변경점 1: 타임아웃 시간 현실적으로 조정
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

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
                    Thread.sleep(3000); // sleep 시간 감소
                }
            }

            if (!pageLoaded) throw new RuntimeException("페이지 로드 최종 실패");

            // 변경점 1: 타임아웃 시간 현실적으로 조정
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

            String year = driver.findElement(By.id("ddlYear")).getAttribute("value");
            String currentMonth = driver.findElement(By.id("ddlMonth")).getAttribute("value");
            int startMonth = fullCrawl ? 1 : Integer.parseInt(currentMonth);
            int endMonth = fullCrawl ? 12 : Integer.parseInt(currentMonth);

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

                    // 변경점 2: DB에 한번에 저장하기 위해 List 생성
                    List<Game> gamesToSave = new ArrayList<>();
                    List<Game> gamesToUpdate = new ArrayList<>();


                    for (WebElement row : rows) {
                        try {
                            List<WebElement> days = row.findElements(By.cssSelector("td.day"));
                            if (!days.isEmpty()) {
                                currentDayRaw = days.get(0).getText().split("\\(")[0].trim();
                            }
                            if (currentDayRaw.isBlank() || !currentDayRaw.matches(".*\\d+.*")) continue;

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
                            boolean hasScore = false; // 점수 유무 플래그

                            // 변경점 3: 팀 이름 파싱 로직을 CANCELED 감지 로직 앞으로 이동
                            String[] vsParts = playText.split("\\s*vs\\s*");
                            if (vsParts.length == 2) {
                                String left = vsParts[0].trim();
                                String right = vsParts[1].trim();

                                Pattern awayPattern = Pattern.compile("([가-힣A-Z]+)\\s*(\\d+)?");
                                Matcher awayMatcher = awayPattern.matcher(left);
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
                            }

                            boolean isCanceledByCss = !row.findElements(By.cssSelector("td.cancel, span.cancel, .cancel")).isEmpty();
                            boolean isCanceledByText = rowText.contains("우천취소") || rowText.contains("경기취소") ||
                                    rowText.contains("기타") || rowText.contains("취소") || rowText.contains("연기") ||
                                    playText.contains("취소") || playText.contains("연기");

                            logger.info("경기 정보 파싱: " + playText + " | 전체 텍스트: " + rowText);

                            if (isCanceledByCss || isCanceledByText) {
                                status = "CANCELED";
                                logger.info("취소된 경기 감지: " + playText);
                            } else {
                                status = determineGameStatus(gameDate, today, hasScore, rowText, hasHighlight);
                            }
                            logger.info("최종 파싱 결과 - Away: " + awayName + "(" + awayScore + ") vs Home: " + homeName + "(" + homeScore + ") | Status: " + status);


                            String awayCode = getTeamCode(awayName);
                            String homeCode = getTeamCode(homeName);
                            if ("XX".equals(awayCode) || "XX".equals(homeCode)) {
                                logger.warning("알 수 없는 팀 코드. 건너뜁니다: " + awayName + " -> " + awayCode + ", " + homeName + " -> " + homeCode);
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

                            // 변경점 4: DB에 바로 저장하지 않고, List에 추가
                            if (fullCrawl) {
                                gamesToSave.add(game); // saveOrUpdateSchedule 대신 일단 리스트에 추가
                            } else {
                                Game existingGame = gameService.findById(gameId).orElse(null);
                                if (existingGame == null) {
                                    gamesToSave.add(game);
                                } else if (shouldUpdateGame(existingGame, game)) {
                                    gamesToUpdate.add(game);
                                }
                            }
                        } catch (Exception rowException) {
                            logger.warning("행 처리 중 오류: " + rowException.getMessage());
                            // continue; // 이어서 계속 진행
                        }
                    }

                    // 변경점 5: 반복문이 끝난 후, 수집된 게임 목록을 한 번에 DB에 저장
                    if (!gamesToSave.isEmpty()) {
                        gameService.saveAllGames(gamesToSave); // GameService에 saveAllGames 같은 메소드 필요
                        logger.info(monthVal + "월 " + gamesToSave.size() + "개의 새로운 게임 저장 완료.");
                    }
                    if (!gamesToUpdate.isEmpty()) {
                        gameService.updateAllGames(gamesToUpdate); // GameService에 updateAllGames 같은 메소드 필요
                        logger.info(monthVal + "월 " + gamesToUpdate.size() + "개의 게임 정보 업데이트 완료.");
                    }

                    logger.info(monthVal + "월 크롤링 완료 - 처리된 게임 수: " + rows.size());
                    // 변경점 6: 불필요한 코드 제거
                    // System.gc();
                    // Thread.sleep(1000);

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
                driver.quit();
                logger.info("WebDriver 정상 종료");
            }
            // System.gc(); // 여기도 제거
        }
        logger.info("크롤링 작업 완료");
    }

    // 이 아래부터는 기존 코드와 거의 동일
    private String determineGameStatus(LocalDate gameDate, LocalDate today, boolean hasScore,
                                       String rowText, boolean hasHighlight) {
        // 이 메소드는 CANCELED가 아닌 경우에만 호출되므로 CANCELED 관련 로직 제거 가능
        if (hasHighlight && hasScore) {
            return "FINISHED";
        }
        if (gameDate.isBefore(today)) {
            return hasScore ? "FINISHED" : "CANCELED"; // 날짜가 지났는데 점수 없으면 취소로 간주
        }
        if (gameDate.isEqual(today) && hasScore) {
            return "IN_PROGRESS"; // 당일 경기인데 점수 있으면 진행중
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