package com.nine.baseballdiary.backend.record;

import com.nine.baseballdiary.backend.game.Game;
import com.nine.baseballdiary.backend.game.GameRepository;
import com.nine.baseballdiary.backend.user.User;
import com.nine.baseballdiary.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            case "KIA 타이거즈": return "KIA";
            case "두산 베어스": return "두산";
            case "롯데 자이언츠": return "롯데";
            case "삼성 라이온즈": return "삼성";
            case "키움 히어로즈": return "키움";
            case "한화 이글스": return "한화";
            case "KT WIZ": return "KT";
            case "LG 트윈스": return "LG";
            case "NC 다이노스": return "NC";
            case "SSG 랜더스": return "SSG";
            default: return favTeam;  // 기본값은 그대로 반환
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
}

