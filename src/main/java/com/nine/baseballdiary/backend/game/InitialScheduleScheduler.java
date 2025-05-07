// src/main/java/com/nine/baseballdiary/backend/game/InitialScheduleScheduler.java
package com.nine.baseballdiary.backend.game;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import jakarta.annotation.PostConstruct; // ✅ 추가

@Component
public class InitialScheduleScheduler {

    private final GameService gameService;
    private static final DateTimeFormatter DB_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");

    public InitialScheduleScheduler(GameService gameService) {
        this.gameService = gameService;
    }


    // ✅ 추가: 서버 부팅 시 바로 1번만 크롤링
    @PostConstruct
    public void initCrawl() {
        try {
            System.out.println("서버 부팅: 초기 스케줄 크롤링 시작");
            weeklyInitialCrawl();
            System.out.println("서버 부팅: 초기 스케줄 크롤링 완료");
        } catch (Exception e) {
            System.err.println("서버 부팅: 초기 스케줄 크롤링 실패");
            e.printStackTrace();
        }
    } // 여기까지 임시로 추가, 아래 크론탭에서 현재 시간으로 변경하여 바로 크롤링 실행 가능:  혜령

    /** 매주 월요일 오전 3시 전체 스케줄(1차) 크롤링 */
    @Scheduled(cron = "0 0 3 ? * MON")
    public void weeklyInitialCrawl() throws Exception {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        try {
            driver.get("https://www.koreabaseball.com/Schedule/Schedule.aspx");
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));
            wait.until(ExpectedConditions.elementToBeClickable(By.id("ddlYear")));
            wait.until(ExpectedConditions.elementToBeClickable(By.id("ddlMonth")));

            String yearTxt = new Select(driver.findElement(By.id("ddlYear")))
                    .getFirstSelectedOption()
                    .getText().trim();
            Select monthSel = new Select(driver.findElement(By.id("ddlMonth")));

            for (int m = 1; m <= 12; m++) {
                monthSel.selectByValue(String.format("%02d", m));
                Thread.sleep(2000);
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#tblScheduleList")));

                List<WebElement> rows = driver.findElements(By.cssSelector("#tblScheduleList tbody tr"));
                for (WebElement row : rows) {
                    List<WebElement> tds = row.findElements(By.tagName("td"));
                    if (tds.size() < 3) continue;

                    // 날짜 파싱
                    String rawDay = row.findElements(By.cssSelector("td.day"))
                            .stream().findFirst().map(WebElement::getText).orElse("");
                    if (rawDay.isBlank()) continue;
                    String dayNum = rawDay.replaceAll("\\(.*?\\)", "")
                            .split("\\.")[1];
                    String dbDate = yearTxt
                            + String.format("%02d", m)
                            + String.format("%02d", Integer.parseInt(dayNum));
                    LocalDate gameDate = LocalDate.parse(dbDate, DB_DATE);

                    // 시간 파싱
                    String timeTxt = tds.get(1).getText().trim();
                    LocalTime startTime = null;
                    if (!timeTxt.isBlank()) {
                        try { startTime = LocalTime.parse(timeTxt, TIME_FMT); }
                        catch (Exception ignore) {}
                    }

                    // 팀 + 취소 여부
                    Document doc = Jsoup.parse(tds.get(2).getAttribute("innerHTML"));
                    Elements spans = doc.select("span");
                    String awayTeam = spans.first().text().trim();
                    String homeTeam = spans.last().text().trim();
                    boolean isCancelled = doc.select("span.cancel").size() > 0
                            || doc.body().text().contains("우천취소");

                    // gameId 생성
                    String gameId = dbDate
                            + getTeamCode(awayTeam)
                            + getTeamCode(homeTeam)
                            + "0";

                    // 저장
                    Game g = new Game();
                    g.setGameId(gameId);
                    g.setDate(gameDate);
                    g.setTime(startTime);
                    g.setPlaytime(null);
                    g.setStadium("");
                    g.setAwayTeam(awayTeam);
                    g.setHomeTeam(homeTeam);
                    g.setAwayScore(0);
                    g.setHomeScore(0);
                    g.setStatus(isCancelled ? "CANCELLED" : "SCHEDULED");
                    g.setAwayImg("");
                    g.setHomeImg("");

                    gameService.saveGame(g);
                    System.out.printf("[INITIAL] %s | %s %s vs %s | %s%n",
                            gameId, dbDate, awayTeam, homeTeam,
                            (isCancelled ? "CANCELLED" : startTime == null ? "TBD" : startTime)
                    );
                }
            }
        } finally {
            driver.quit();
        }
        System.out.println("초기 크롤링 완료.");
    }

    private static String getTeamCode(String name) {
        return switch (name) {
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
            default     -> "XX";
        };
    }
}
