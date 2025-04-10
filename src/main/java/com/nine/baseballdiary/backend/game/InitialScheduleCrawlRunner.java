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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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
        if (!initialCrawlEnabled) {
            System.out.println("Initial crawling is disabled.");
            return;
        }

        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();

        try {
            driver.get("https://www.koreabaseball.com/Schedule/Schedule.aspx");
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ddlYear")));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ddlMonth")));

            WebElement yearSelectElement = driver.findElement(By.id("ddlYear"));
            Select selectYear = new Select(yearSelectElement);
            String selectedYear = selectYear.getFirstSelectedOption().getText().trim();

            WebElement monthSelectElement = driver.findElement(By.id("ddlMonth"));
            Select selectMonth = new Select(monthSelectElement);

            for (int m = 1; m <= 12; m++) {
                selectMonth.selectByValue(String.format("%02d", m));
                String selectedMonth = String.format("%02d", m);
                Thread.sleep(2000);
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#tblScheduleList")));

                WebElement scheduleTable = driver.findElement(By.cssSelector("#tblScheduleList"));
                List<WebElement> rows = scheduleTable.findElements(By.cssSelector("tbody tr"));
                System.out.println("현재 월: " + selectedMonth + " - 행 수: " + rows.size());

                DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                String currentDayRaw = "";

                for (WebElement row : rows) {
                    List<WebElement> tds = row.findElements(By.tagName("td"));
                    if (tds.isEmpty()) continue;

                    List<WebElement> dayCells = row.findElements(By.cssSelector("td.day"));
                    if (!dayCells.isEmpty()) {
                        currentDayRaw = dayCells.get(0).getText().trim();
                    }
                    if (currentDayRaw.isEmpty()) continue;

                    List<WebElement> playCells = row.findElements(By.cssSelector("td.play"));
                    if (playCells.isEmpty()) continue;
                    String playHtml = playCells.get(0).getAttribute("innerHTML");
                    Document doc = Jsoup.parse(playHtml);
                    Elements spans = doc.select("span");
                    if (spans.size() < 2) continue;
                    String awayTeam = spans.first().text().trim();
                    String homeTeam = spans.last().text().trim();

                    String dayPartRaw = currentDayRaw.replaceAll("\\(.*\\)", "").trim();
                    String[] parts = dayPartRaw.split("\\.");
                    if (parts.length < 2) continue;
                    String dayPart = parts[1].length() == 1 ? "0" + parts[1] : parts[1];

                    String dbDateStr = selectedYear + selectedMonth + dayPart;
                    LocalDate gameDate = LocalDate.parse(dbDateStr, dbFormatter);

                    String gameId = dbDateStr + getTeamCode(homeTeam) + getTeamCode(awayTeam) + "0";

                    Game game = new Game();
                    game.setGameId(gameId);
                    game.setDate(gameDate);
                    game.setTime(null);
                    game.setPlaytime(null);
                    game.setStadium("");
                    game.setHomeTeam(homeTeam);
                    game.setAwayTeam(awayTeam);
                    game.setHomeScore(0);
                    game.setAwayScore(0);
                    game.setStatus("SCHEDULED");
                    game.setHomeImg("");
                    game.setAwayImg("");

                    gameService.saveGame(game);
                    System.out.printf("[DB 저장] 월: %s, 날짜: %s, 홈팀: %s, 원정팀: %s, gameId: %s%n",
                            selectedMonth, currentDayRaw, homeTeam, awayTeam, gameId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
        System.out.println("초기 1단계 크롤링 완료.");
    }

    private static String getTeamCode(String teamName) {
        switch (teamName) {
            case "두산": return "DS";
            case "NC": return "NC";
            case "KT": return "KT";
            case "KIA": return "KIA";
            case "한화": return "HH";
            case "LG": return "LG";
            case "키움": return "WO";
            case "삼성": return "SS";
            case "롯데": return "LT";
            case "SSG": return "SK";
            default: return "XX";
        }
    }
}
