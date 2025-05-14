package com.nine.baseballdiary.backend.record;

import com.nine.baseballdiary.backend.game.Game;
import com.nine.baseballdiary.backend.game.GameRepository;
import com.nine.baseballdiary.backend.user.User;
import com.nine.baseballdiary.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RecordService {
    private final RecordRepository recordRepo;
    private final GameRepository gameRepo;
    private final UserRepository userRepo;

    // 레코드 생성 메서드
    @Transactional
    public RecordResponse createRecord(CreateRecordRequest req) {
        // 1) 유저·게임 정보 조회
        User user = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저 ID: " + req.getUserId()));

        // 사용자 favTeam이 없으면 예외 처리
        if (user.getFavTeam() == null || user.getFavTeam().isEmpty()) {
            throw new IllegalArgumentException("유저의 즐겨찾는 팀이 설정되지 않았습니다.");
        }

        Game game = gameRepo.findById(req.getGameId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게임 ID: " + req.getGameId()));

        // 2) emotion_code 값을 그대로 할당 (변환 없이 그대로 사용)
        Integer emotionCode = req.getEmotionCode();

        // 3) 경기 결과 계산
        String result = calculateResult(user.getFavTeam(), game);

        // 4) Record 엔티티 빌드 (game 필드들은 DB 조회값 사용)
        Record record = Record.builder()
                .userId(req.getUserId())
                .gameId(req.getGameId())
                .seatInfo(req.getSeatInfo())
                .ticketImageUrl(req.getTicketImageUrl())
                .emotionCode(emotionCode)  // emotion_code에 값 할당
                .comment(req.getComment())
                .bestPlayer(req.getBestPlayer())
                .foodTags(req.getFoodTags())
                .mediaUrls(req.getMediaUrls())
                .result(result)      // 서버에서 계산한 결과
                .build();

        record = recordRepo.save(record);
        return toResponse(record);  // Record를 RecordResponse로 변환하여 반환
    }


    // `fav_team`을 짧은 이름으로 변환하는 메서드
    private String convertFavTeam(String favTeam) {
        switch (favTeam) {
            case "KIA 타이거즈":
                return "KIA";
            case "두산 베어스":
                return "두산";
            case "롯데 자이언츠":
                return "롯데";
            case "삼성 라이온즈":
                return "삼성";
            case "키움 히어로즈":
                return "키움";
            case "한화 이글스":
                return "한화";
            case "KT WIZ":
                return "KT";
            case "LG 트윈스":
                return "LG";
            case "NC 다이노스":
                return "NC";
            case "SSG 랜더스":
                return "SSG";
            default:
                return favTeam;  // 기본값은 그대로 반환
        }
    }

    // `fav_team`과 `home_team`, `away_team` 비교 후 경기 결과 계산
    private String calculateResult(String favTeam, Game game) {
        // `fav_team`을 짧은 이름으로 변환
        String convertedFavTeam = convertFavTeam(favTeam);
        String homeTeam = game.getHomeTeam();
        String awayTeam = game.getAwayTeam();

        // `home_team`과 `away_team`을 비교
        boolean isHome = convertedFavTeam.equals(homeTeam);
        boolean isAway = convertedFavTeam.equals(awayTeam);

        if (!isHome && !isAway) {
            // 즐겨찾는 팀이 경기 참가팀이 아닌 경우 예외 처리
            throw new IllegalArgumentException("유저 즐겨찾는 팀이 해당 경기 정보에 없습니다.");
        }

        // 경기 스코어 비교 (홈팀과 원정팀 스코어)
        int homeScore = game.getHomeScore() != null ? game.getHomeScore() : 0;
        int awayScore = game.getAwayScore() != null ? game.getAwayScore() : 0;

        // 동점일 경우 DRAW
        if (homeScore == awayScore) {
            return "DRAW";
        }

        // 홈팀/원정팀 승리 계산
        boolean isWin = (isHome && homeScore > awayScore)
                || (isAway && awayScore > homeScore);
        return isWin ? "WIN" : "LOSE";  // WIN / LOSE / DRAW
    }

    // Record를 RecordResponse로 변환
    private RecordResponse toResponse(Record record) {
        return new RecordResponse(
                record.getRecordId(),
                record.getUserId(),
                record.getGameId(),
                record.getSeatInfo(),
                record.getTicketImageUrl(),
                record.getEmotionCode(),
                record.getComment(),
                record.getBestPlayer(),
                record.getFoodTags(),
                record.getMediaUrls(),
                record.getResult(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }


    // 레코드 상세 정보 조회
    public RecordDetailResponse getRecordDetail(Long recordId) {
        // 1) 레코드 조회
        Record record = recordRepo.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 레코드 ID: " + recordId));

        // 2) 게임 정보 조회 (gameId를 Long 타입으로 조회)
        Game game = gameRepo.findById(record.getGameId())  // gameId는 String으로 저장되므로 그대로 사용
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게임 ID: " + record.getGameId()));

        // 3) 게임 일자, 시간, 홈팀, 어웨이팀, 구장 정보 가져오기
        String gameDate = game.getDate().toString();    // 날짜
        String gameTime = game.getTime().toString();    // 시간
        String homeTeam = convertHomeTeam(game.getHomeTeam());  // 홈팀 변환
        String awayTeam = convertAwayTeam(game.getAwayTeam());  // 어웨이팀 변환
        String stadium = convertStadium(game.getStadium());     // 구장 변환

        // 4) 이모티콘 코드만 반환
        Integer emotionCode = record.getEmotionCode();  // 이모티콘 코드만 반환
        String emotionLabel = convertEmotionLabel(emotionCode);  // 이모티콘 라벨 변환

        // 5) 응답 반환
        return new RecordDetailResponse(
                gameDate,
                gameTime,
                emotionCode,
                emotionLabel,  // 라벨 값 추가
                record.getTicketImageUrl(),
                homeTeam,
                awayTeam,
                stadium,
                record.getSeatInfo()
        );
    }

    // 이모티콘 코드에 해당하는 라벨 반환
    private String convertEmotionLabel(Integer emotionCode) {
        switch (emotionCode) {
            case 1: return "짜릿해요";
            case 2: return "만족해요";
            case 3: return "감동이에요";
            case 4: return "놀랐어요";
            case 5: return "행복해요";
            case 6: return "답답해요";
            case 7: return "아쉬워요";
            case 8: return "화났어요";
            case 9: return "지쳤어요";
            default: return "알 수 없음";
        }
    }

    // `fav_team`을 짧은 이름으로 변환하는 메서드
    private String convertHomeTeam(String homeTeam) {
        switch (homeTeam) {
            case "KIA": return "KIA 타이거즈";
            case "두산": return "두산 베어스";
            case "롯데": return "롯데 자이언츠";
            case "삼성": return "삼성 라이온즈";
            case "키움": return "키움 히어로즈";
            case "한화": return "한화 이글스";
            case "KT": return "KT WIZ";
            case "LG": return "LG 트윈스";
            case "NC": return "NC 다이노스";
            case "SSG": return "SSG 랜더스";
            default: return homeTeam;  // 기본값은 그대로 반환
        }
    }

    // 어웨이팀 값을 짧은 이름에서 긴 이름으로 변환하는 메서드
    private String convertAwayTeam(String awayTeam) {
        switch (awayTeam) {
            case "KIA": return "KIA 타이거즈";
            case "두산": return "두산 베어스";
            case "롯데": return "롯데 자이언츠";
            case "삼성": return "삼성 라이온즈";
            case "키움": return "키움 히어로즈";
            case "한화": return "한화 이글스";
            case "KT": return "KT WIZ";
            case "LG": return "LG 트윈스";
            case "NC": return "NC 다이노스";
            case "SSG": return "SSG 랜더스";
            default: return awayTeam;  // 기본값은 그대로 반환
        }
    }

    // stadium 값을 짧은 이름에서 긴 이름으로 변환하는 메서드
    private String convertStadium(String stadium) {
        switch (stadium) {
            case "잠실": return "잠실야구장";
            case "문학": return "문학야구장";
            case "고척": return "고척 SKYDOME";
            case "사직": return "사직야구장";
            case "수원": return "KT 위즈 파크";
            case "대전(신)": return "한화생명 이글스 파크";
            case "대구": return "대구삼성라이온즈파크";
            case "광주": return "기아 챔피언스 필드";
            case "창원": return "NC 파크";
            default: return stadium;  // 기본값은 그대로 반환
        }
    }

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd EEE", Locale.ENGLISH);

    // 1) 피드
    public List<RecordFeedResponse> getUserRecordsFeed(Long userId) {
        return recordRepo.findByUserId(userId).stream()
                .filter(rec -> rec.getMediaUrls() != null && !rec.getMediaUrls().isEmpty())
                .map(rec -> {
                    Game g = gameRepo.findById(rec.getGameId()).orElseThrow();
                    // 바뀐 포맷 사용
                    String fmtDate = g.getDate().format(DATE_FMT);
                    String img = rec.getMediaUrls().get(0);
                    return new RecordFeedResponse(rec.getRecordId(), fmtDate, img);
                })
                .collect(Collectors.toList());
    }


    //2) 리스트: 업로드 상세 정보와 동일
    public List<RecordListResponse> getUserRecordsList(Long userId) {
        return recordRepo.findByUserId(userId).stream()
                .map(rec -> {
                    // 1) Game 조회
                    Game game = gameRepo.findById(rec.getGameId())
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게임 ID: " + rec.getGameId()));
                    // 2) 날짜 포맷
                    String gameDate = game.getDate().format(DATE_FMT);
                    // 3) 시간
                    String gameTime = game.getTime().toString();
                    // 4) 감정 라벨
                    Integer emoCode = rec.getEmotionCode();
                    String emoLabel = convertEmotionLabel(emoCode);
                    // 5) 팀/구장 변환
                    String home = convertHomeTeam(game.getHomeTeam());
                    String away = convertAwayTeam(game.getAwayTeam());
                    String stdm = convertStadium(game.getStadium());
                    // 6) 결과
                    String result = rec.getResult();

                    return new RecordListResponse(
                            gameDate,
                            gameTime,
                            emoCode,
                            emoLabel,
                            rec.getTicketImageUrl(),
                            home,
                            away,
                            stdm,
                            rec.getSeatInfo(),
                            result
                    );
                })
                .collect(Collectors.toList());
    }

    // 3) 캘린더: 날짜 + 승패여부
    public List<RecordCalendarResponse> getUserRecordsCalendar(Long userId) {
        return recordRepo.findByUserId(userId).stream()
                .map(rec -> {
                    Game g = gameRepo.findById(rec.getGameId())
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게임 ID: " + rec.getGameId()));
                    // LocalDate → ISO 문자열 ("yyyy-MM-dd")
                    String gameDateStr = g.getDate().toString();
                    return new RecordCalendarResponse(gameDateStr, rec.getResult());
                })
                .collect(Collectors.toList());
    }

}