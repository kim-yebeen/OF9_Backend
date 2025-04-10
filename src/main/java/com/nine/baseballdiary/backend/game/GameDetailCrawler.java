package com.nine.baseballdiary.backend.game;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
public class GameDetailCrawler {

    /**
     * 상세정보 크롤링 메서드
     * @param gameDate "yyyyMMdd" 형식의 날짜 문자열
     * @param gameId   크롤링해서 생성한 gameId
     * @return DetailInfo 객체에 크롤링한 상세 정보를 담아 반환,
     *         경기 전(또는 상세정보 없음)인 경우 null 반환
     */
    public DetailInfo crawlDetail(String gameDate, String gameId) {
        // 상세정보 페이지 URL 생성
        String url = "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx?gameDate="
                + gameDate + "&gameId=" + gameId + "&section=REVIEW";
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .get();

            // 예: 페이지에 "경기 대기중" 또는 SCHEDULED 표시가 있다면 아직 경기 전임
            if(doc.text().contains("경기 대기중") || doc.text().contains("SCHEDULED")) {
                System.out.println("[상세정보] 경기 전이므로 상세 정보가 없습니다. URL: " + url);
                return null;
            }

            // 상세정보 파싱 (실제 셀렉터는 개발자 도구(F12)로 확인 후 변경)
            DetailInfo detail = new DetailInfo();
            // 아래 css 셀렉터 부분은 예시입니다.
            String playtimeStr = doc.select("div.playTime").text().trim();       // ex: "18:30:00"
            String stadium = doc.select("div.stadium").text().trim();              // ex: "잠실"
            String homeScoreStr = doc.select("span.homeScore").text().trim();      // ex: "5"
            String awayScoreStr = doc.select("span.awayScore").text().trim();      // ex: "3"
            String status = doc.select("div.status").text().trim();                // ex: "FINISHED"

            detail.setPlaytime(playtimeStr);
            detail.setStadium(stadium);
            detail.setHomeScore(parseScore(homeScoreStr));
            detail.setAwayScore(parseScore(awayScoreStr));
            detail.setStatus(status);

            return detail;
        } catch (Exception e) {
            System.out.println("[상세정보] 크롤링 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private int parseScore(String scoreStr) {
        try {
            return Integer.parseInt(scoreStr);
        } catch (Exception e) {
            return 0;
        }
    }
}

