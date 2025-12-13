package com.nine.baseballdiary.backend.search.dto;

import com.nine.baseballdiary.backend.game.Game;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.record.GameRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Getter
@Builder
public class SearchRecordDto {
    private Long recordId;
    private Long authorId;
    private String authorNickname;
    private String authorProfileImage;
    private String authorFavTeam;
    private FollowStatus followStatus;  // ✅ 추가
    private String gameDate;
    private String gameTime;
    private String homeTeam;
    private String awayTeam;
    private Integer homeScore;
    private Integer awayScore;
    private String stadium;
    private Integer emotionCode;
    private String emotionLabel;
    private String comment;
    private String longContent;
    private String result;
    private List<String> mediaUrls;
    private String createdAt;
    private Long likeCount;
    private Boolean isLiked;
    private Long commentCount;

    public static SearchRecordDto from(GameRecord record, Game game, User author,
                                       Long likeCount, Boolean isLiked, Long commentCount,
                                       FollowStatus followStatus) {  // ✅ 파라미터 추가
        return SearchRecordDto.builder()
                .recordId(record.getRecordId())
                .authorId(author.getId())
                .authorNickname(author.getNickname() != null ? author.getNickname() : "알 수 없음")
                .authorProfileImage(author.getProfileImageUrl())
                .authorFavTeam(author.getFavTeam() != null ? author.getFavTeam() : "")
                .followStatus(followStatus)  // ✅ 추가
                .gameDate(game.getDate().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E)요일", Locale.KOREAN)))
                .gameTime(game.getTime() != null ? game.getTime().format(DateTimeFormatter.ofPattern("H:mm")) : "")
                .homeTeam(convertTeamName(game.getHomeTeam()))
                .awayTeam(convertTeamName(game.getAwayTeam()))
                .homeScore(game.getHomeScore() != null ? game.getHomeScore() : 0)
                .awayScore(game.getAwayScore() != null ? game.getAwayScore() : 0)
                .stadium(convertStadiumName(game.getStadium()))
                .emotionCode(record.getEmotionCode())
                .emotionLabel(convertEmotionLabel(record.getEmotionCode()))
                .comment(record.getComment() != null ? record.getComment() : "")
                .longContent(record.getLongContent() != null ? record.getLongContent() : "")
                .result(record.getResult() != null ? record.getResult() : "")
                .mediaUrls(record.getMediaUrls() != null ? record.getMediaUrls() : Collections.emptyList())
                .createdAt(record.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .likeCount(likeCount)
                .isLiked(isLiked)
                .commentCount(commentCount)
                .build();
    }

    private static String convertTeamName(String teamCode) {
        return switch (teamCode) {
            case "KIA" -> "KIA 타이거즈";
            case "두산" -> "두산 베어스";
            case "롯데" -> "롯데 자이언츠";
            case "삼성" -> "삼성 라이온즈";
            case "키움" -> "키움 히어로즈";
            case "한화" -> "한화 이글스";
            case "KT" -> "KT WIZ";
            case "LG" -> "LG 트윈스";
            case "NC" -> "NC 다이노스";
            case "SSG" -> "SSG 랜더스";
            default -> teamCode;
        };
    }

    private static String convertStadiumName(String stadiumCode) {
        return switch (stadiumCode) {
            case "잠실" -> "잠실야구장";
            case "문학" -> "문학야구장";
            case "고척" -> "고척 SKYDOME";
            case "사직" -> "사직야구장";
            case "수원" -> "KT 위즈 파크";
            case "대전(신)" -> "한화생명 이글스 파크";
            case "대구" -> "대구삼성라이온즈파크";
            case "광주" -> "기아 챔피언스 필드";
            case "창원" -> "NC 파크";
            default -> stadiumCode;
        };
    }

    private static String convertEmotionLabel(int code) {
        return switch (code) {
            case 1 -> "짜릿해요";
            case 2 -> "만족해요";
            case 3 -> "감동이에요";
            case 4 -> "놀랐어요";
            case 5 -> "행복해요";
            case 6 -> "답답해요";
            case 7 -> "아쉬워요";
            case 8 -> "화났어요";
            case 9 -> "지쳤어요";
            default -> "알 수 없음";
        };
    }
}