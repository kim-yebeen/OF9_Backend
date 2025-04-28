package com.nine.baseballdiary.backend.game;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
@Component
public class InitialScheduleCrawlRunner implements CommandLineRunner {

    @Value("${initial.crawl.enabled:true}")
    private boolean initialCrawlEnabled;
    private final GameService gameService;

    public InitialScheduleCrawlRunner(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!initialCrawlEnabled) return;

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions()
                .addArguments("--headless", "--disable-gpu", "--window-size=1920,1080");
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("https://www.koreabaseball.com/Schedule/Schedule.aspx");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // 년도/월 선택 대기
            wait.until(ExpectedConditions.elementToBeClickable(By.id("ddlYear")));
            wait.until(ExpectedConditions.elementToBeClickable(By.id("ddlMonth")));

            Select yearSelect  = new Select(driver.findElement(By.id("ddlYear")));
            String year = yearSelect.getFirstSelectedOption().getText().trim();

            Select monthSelect = new Select(driver.findElement(By.id("ddlMonth")));

            for (int m = 1; m <= 12; m++) {
                String mm = String.format("%02d", m);

                // 월 변경
                monthSelect.selectByValue(mm);

                // → monthSelect가 바뀌고 나면
                //    테이블 로우가 1개 이상 로드될 때까지 대기
                wait.until(driver1 -> {
                    List<WebElement> rows = driver1.findElements(
                            By.cssSelector("table#tblScheduleList tbody tr"));
                    return rows.size() > 0;
                });

                List<WebElement> rows = driver.findElements(
                        By.cssSelector("table#tblScheduleList tbody tr"));
                System.out.printf("현재 월: %s - 로우 수: %d%n", mm, rows.size());

                for (WebElement row : rows) {
                    // 날짜/팀 파싱 로직 (기존 코드 그대로)
                    List<WebElement> dayCells  = row.findElements(By.cssSelector("td.day"));
                    List<WebElement> playCells = row.findElements(By.cssSelector("td.play"));
                    if (dayCells.isEmpty() || playCells.isEmpty()) continue;

                    // ex) "03.22(토)" → "03.22" → ["03","22"]
                    String rawDay = dayCells.get(0).getText()
                            .replaceAll("\\(.*\\)", "").trim();
                    String[] parts = rawDay.split("\\.");
                    String dayPart  = parts[1].length()==1? "0"+parts[1] : parts[1];
                    String dbDate   = year + mm + dayPart;

                    // playCells 에서 span 텍스트로 팀명 추출
                    String html    = playCells.get(0).getAttribute("innerHTML");
                    Document doc   = Jsoup.parse(html);
                    Elements spans = doc.select("span");
                    if (spans.size() < 2) continue;
                    String away = spans.first().text().trim();
                    String home = spans.last().text().trim();

                    String gameId = dbDate
                            + getTeamCode(away)
                            + getTeamCode(home)
                            + "0";

                    Game g = new Game();
                    g.setGameId(gameId);
                    g.setDate(LocalDate.parse(dbDate,
                            DateTimeFormatter.ofPattern("yyyyMMdd")));
                    g.setTime(null);
                    g.setPlaytime(null);
                    g.setStadium("");
                    g.setAwayTeam(away);
                    g.setHomeTeam(home);
                    g.setAwayScore(0);
                    g.setHomeScore(0);
                    g.setStatus("SCHEDULED");
                    g.setAwayImg("");
                    g.setHomeImg("");
                    gameService.saveGame(g);

                    System.out.printf("[저장] %s | %s vs %s%n",
                            gameId, away, home);
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
