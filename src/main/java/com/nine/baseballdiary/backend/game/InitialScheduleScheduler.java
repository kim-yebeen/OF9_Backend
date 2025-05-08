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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class InitialScheduleScheduler {

    private final GameService gameService;
    private static final DateTimeFormatter DB_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");

    public InitialScheduleScheduler(GameService gameService) {
        this.gameService = gameService;
    }


    /** 매주 월요일 오전 3시 전체 스케줄(1차) 크롤링 */
    @Scheduled(cron = "0 30 17 * * ?")
    public void weeklyInitialCrawl() {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        try {
            driver.get("https://www.koreabaseball.com/Schedule/Schedule.aspx");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            String year = driver.findElement(By.id("ddlYear")).getAttribute("value");

            // 1월부터 12월까지 순회
            for (int m = 1; m <= 12; m++) {
                String monthVal = String.format("%02d", m);
                // 반드시 loop 안에서 새로 찾기
                Select monthSelect = new Select(driver.findElement(By.id("ddlMonth")));
                monthSelect.selectByValue(monthVal);

                // AJAX로 테이블이 업데이트 될 때까지 대기
                wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                        By.cssSelector("#tblScheduleList tbody tr"), 0
                ));

                // 로드된 모든 행을 다시 읽어서 크롤링
                List<WebElement> rows = driver.findElements(
                        By.cssSelector("#tblScheduleList tbody tr")
                );
                String currentDayRaw = "";
                for (WebElement row : rows) {
                    // (1) 날짜 셀 (rowspan 사용하는 월 첫 행만)
                    List<WebElement> days = row.findElements(By.cssSelector("td.day"));
                    if (!days.isEmpty()) {
                        currentDayRaw = days.get(0).getText().split("\\(")[0].trim();
                    }
                    if (currentDayRaw.isBlank()) continue;

                    // (2) 경기 정보
                    WebElement playCell = row.findElement(By.cssSelector("td.play"));
                    Document doc = Jsoup.parse(playCell.getAttribute("innerHTML"));
                    Elements spans = doc.select("span");
                    if (spans.size() < 2) continue;
                    String awayName = spans.first().text().trim();
                    String homeName = spans.last().text().trim();

                    // (3) gameDate
                    String dayPart = currentDayRaw.split("\\.")[1];
                    String dbDateStr = year + monthVal + String.format("%02d", Integer.parseInt(dayPart));
                    LocalDate gameDate = LocalDate.parse(dbDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));

                    // (4) 시간
                    String timeText = row.findElement(By.cssSelector("td.time")).getText().trim();
                    if (timeText.isBlank()) continue;
                    LocalTime startTime = LocalTime.parse(timeText, DateTimeFormatter.ofPattern("H:mm"));

                    // (5) 구장
                    List<WebElement> tds = row.findElements(By.tagName("td"));
                    String stadium = tds.size() >= 8
                            ? tds.get(tds.size() - 2).getText().trim()
                            : "";

                    // (6) gameId 조합
                    String gameId = dbDateStr
                            + getTeamCode(awayName)
                            + getTeamCode(homeName)
                            + "0";

                    // (7) 저장
                    Game g = new Game();
                    g.setGameId(gameId);
                    g.setDate(gameDate);
                    g.setTime(startTime);
                    g.setPlaytime(null);
                    g.setStadium(stadium);
                    g.setAwayTeam(awayName);
                    g.setHomeTeam(homeName);
                    g.setAwayScore(0);
                    g.setHomeScore(0);
                    g.setStatus("SCHEDULED");
                    gameService.saveGame(g);
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
