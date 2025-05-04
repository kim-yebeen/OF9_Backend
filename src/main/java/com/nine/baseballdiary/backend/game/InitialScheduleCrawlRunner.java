package com.nine.baseballdiary.backend.game;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class InitialScheduleCrawlRunner implements CommandLineRunner {

    private final GameService gameService;
    private static final DateTimeFormatter DB_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");

    public InitialScheduleCrawlRunner(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void run(String... args) throws Exception {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        try {
            driver.get("https://www.koreabaseball.com/Schedule/Schedule.aspx");
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));
            wait.until(ExpectedConditions.elementToBeClickable(By.id("ddlYear")));
            wait.until(ExpectedConditions.elementToBeClickable(By.id("ddlMonth")));

            Select yearSel  = new Select(driver.findElement(By.id("ddlYear")));
            String yearTxt  = yearSel.getFirstSelectedOption().getText().trim();

            WebElement monthElem = driver.findElement(By.id("ddlMonth"));
            Select monthSel = new Select(monthElem);

            for (int m = 1; m <= 12; m++) {
                monthSel.selectByValue(String.format("%02d", m));
                Thread.sleep(2000);
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#tblScheduleList")));

                WebElement table = driver.findElement(By.cssSelector("#tblScheduleList"));
                List<WebElement> rows = table.findElements(By.cssSelector("tbody tr"));

                for (WebElement row : rows) {
                    List<WebElement> tds = row.findElements(By.tagName("td"));
                    if (tds.size() < 3) continue;

                    // 1) 날짜
                    String rawDay = row.findElements(By.cssSelector("td.day")).stream()
                            .findFirst()
                            .map(WebElement::getText)
                            .orElse("");
                    if (rawDay.isBlank()) continue;
                    String dayNum = rawDay.replaceAll("\\(.*?\\)", "").split("\\.")[1];
                    String dbDate = yearTxt + String.format("%02d", m) + String.format("%02d", Integer.parseInt(dayNum));
                    LocalDate gameDate = LocalDate.parse(dbDate, DB_DATE);

                    // 2) 시작 시간
                    String timeTxt = tds.get(1).getText().trim();
                    LocalTime startTime = null;
                    if (!timeTxt.isBlank()) {
                        try {
                            startTime = LocalTime.parse(timeTxt, TIME_FMT);
                        } catch (Exception ignore) { }
                    }

                    // 3) 홈/어웨이 + 취소 여부
                    String playHtml = tds.get(2).getAttribute("innerHTML");
                    Document doc = Jsoup.parse(playHtml);
                    Elements spans = doc.select("span");
                    String awayTeam = spans.first().text().trim();
                    String homeTeam = spans.last().text().trim();
                    boolean isCancelled = !doc.select("span.cancel").isEmpty();

                    // 4) gameId 생성 (기존 규칙)
                    String gameId = dbDate
                            + getTeamCode(awayTeam)
                            + getTeamCode(homeTeam)
                            + "0";

                    // 5) 엔티티 저장
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
                            (isCancelled ? "CANCELLED" : startTime == null ? "TBD" : startTime));
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
