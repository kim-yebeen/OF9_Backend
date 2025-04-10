package com.nine.baseballdiary.backend.game;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class GameDetailService {

    private final GameRepository gameRepository;

    public void updateGameDetail(String gameId, String gameDate) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            System.out.println("❌ 해당 gameId를 찾을 수 없습니다: " + gameId);
            return;
        }
        updateGameDetails(game);
    }

    @Transactional
    public void updateGameDetails(Game game) {
        try {
            String gameId = game.getGameId();
            String date = game.getDate().toString(); // yyyy-MM-dd 형식
            String formattedDate = date.replace("-", ""); // yyyyMMdd

            String url = "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx?gameId=" + gameId + "&gameDate=" + formattedDate;

            Document doc = Jsoup.connect(url).get();

            // 경기장
            Element stadiumEl = doc.selectFirst("div.score_box dl dd");
            if (stadiumEl != null) {
                String stadium = stadiumEl.text().trim();
                game.setStadium(stadium);
            }

            // 경기 시작 시간
            Element timeEl = doc.selectFirst("div.score_box p.date span");
            if (timeEl != null) {
                String timeText = timeEl.text().trim(); // 예: "18:30"
                if (timeText.matches("\\d{2}:\\d{2}")) {
                    game.setTime(LocalTime.parse(timeText, DateTimeFormatter.ofPattern("HH:mm")));
                }
            }

            // 경기 종료 시간 → USE_TM 항목을 활용해 추출
            Element scriptEl = doc.select("script").stream()
                    .filter(e -> e.html().contains("USE_TM"))
                    .findFirst()
                    .orElse(null);

            if (scriptEl != null) {
                String script = scriptEl.html();
                String useTm = extractValue(script, "USE_TM");
                if (useTm != null && useTm.matches("\\d{1,2}:\\d{2}")) {
                    game.setPlaytime(LocalTime.parse(useTm, DateTimeFormatter.ofPattern("H:mm")));
                }
            }

            // 경기 상태 변경
            if (game.getTime() != null && game.getPlaytime() != null) {
                game.setStatus("FINISHED");
            }

            gameRepository.save(game);
            System.out.println("✅ 상세 업데이트 완료 - " + game.getGameId());

        } catch (Exception e) {
            System.out.println("❗ 상세 업데이트 실패: " + game.getGameId());
            e.printStackTrace();
        }
    }

    private String extractValue(String script, String key) {
        String marker = "\"" + key + "\":\"";
        int start = script.indexOf(marker);
        if (start == -1) return null;
        int end = script.indexOf("\"", start + marker.length());
        return script.substring(start + marker.length(), end);
    }
}
