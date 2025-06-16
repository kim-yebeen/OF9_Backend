package com.nine.baseballdiary.backend.record;

import com.nine.baseballdiary.backend.game.Game;
import com.nine.baseballdiary.backend.game.GameRepository;
import com.nine.baseballdiary.backend.reaction.ReactionService;
import com.nine.baseballdiary.backend.reaction.ReactionStatsResponse;
import com.nine.baseballdiary.backend.user.dto.UserDto;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.repository.UserFollowRepository;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nine.baseballdiary.backend.reaction.RecordReactionSummary;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RecordService {
    private final RecordRepository recordRepo;
    private final GameRepository   gameRepo;
    private final UserRepository   userRepo;
    private final UserFollowRepository userflRepo;
    private final ReactionService reactionService;

    // 피드, 리스트에서 짧게 보여줄 때  —  "25/04/29 Fri"
    private static final DateTimeFormatter FEED_FMT =
            DateTimeFormatter.ofPattern("yy/MM/dd EEE", Locale.ENGLISH);

    // 업로드 후 상세에 “2025년 04월 29일 (금)요일” 처럼 보여줄 때
    private static final DateTimeFormatter UPLOAD_FMT =
            DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E)요일", Locale.KOREAN);

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("H:mm");


    // 레코드 업로드 (모든 정보를 한번에 처리)
    @Transactional
    public RecordUploadResponse uploadRecord(Long userId, CreateRecordRequest req) {
        // 1) User 조회
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저: " + req.getUserId()));

        // 2) Game 조회
        Game game = gameRepo.findById(req.getGameId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게임: " + req.getGameId()));

        // 3) 결과 계산
        String result = calculateResult(user.getFavTeam(), game);

        // 4) Record 엔티티 빌드 (모든 정보 포함)
        Record record = Record.builder()
                .userId(userId)
                .gameId(req.getGameId())
                .stadium(req.getStadium())
                .seatInfo(req.getSeatInfo())
                .emotionCode(req.getEmotionCode())
                .comment(req.getComment())
                .longContent(req.getLongContent())
                .bestPlayer(req.getBestPlayer())
                .companions(req.getCompanions())
                .foodTags(req.getFoodTags())
                .mediaUrls(req.getMediaUrls())
                .result(result)
                .build();

        // 5) 저장
        Record savedRecord = recordRepo.save(record);

        // 6) 단순한 응답 반환 (recordId와 gameDate만)
        String dateStr = game.getDate().format(UPLOAD_FMT);
        return new RecordUploadResponse(savedRecord.getRecordId(), dateStr);
    }

    // 레코드 수정
    @Transactional
    public RecordDetailResponse updateRecord(Long currentUserId, Long recordId, UpdateRecordRequest req) {
        Record rec = recordRepo.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 레코드"));
        if (!rec.getUserId().equals(currentUserId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "기록을 수정할 권한이 없습니다.");
        // 선택입력값만 set
        rec.setComment   (req.getComment());
        rec.setLongContent(req.getLongContent());
        rec.setBestPlayer(req.getBestPlayer());
        rec.setCompanions(req.getCompanions());
        rec.setFoodTags  (req.getFoodTags());
        rec.setMediaUrls (req.getMediaUrls());
        // 변경감지 → 자동 save
        return getRecordDetail(recordId);
    }

    // 2) 피드에서 클릭 시 상세 조회
    @Transactional(readOnly = true)
    public RecordDetailResponse getRecordDetail(Long recordId) {
        Record rec = recordRepo.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 레코드 ID: " + recordId));
        Game game = gameRepo.findById(rec.getGameId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게임 ID: " + rec.getGameId()));

        // 1. record에서 친구 ID 목록 (List<Long>)을 가져옵니다.
        List<Long> companionIds = rec.getCompanions();
        List<UserDto> companionDetails = List.of(); // 기본값은 빈 리스트

        // 2. 친구 ID 목록이 비어있지 않은 경우에만 DB를 조회하여 UserDto 목록으로 변환합니다.
        if (companionIds != null && !companionIds.isEmpty()) {
            companionDetails = userRepo.findAllById(companionIds).stream()
                    .map(companionUser -> new UserDto(
                            companionUser.getId(),
                            companionUser.getNickname(),
                            companionUser.getProfileImageUrl(),
                            companionUser.getFavTeam()
                    ))
                    .collect(Collectors.toList());
        }
        String fmtDate = game.getDate().format(UPLOAD_FMT);
        String fmtTime = game.getTime().format(TIME_FMT);
        String emoLabel = convertEmotionLabel(rec.getEmotionCode());

        String createdAtStr = rec.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        RecordReactionSummary summary = reactionService.getSummary(recordId);
        List<ReactionStatsResponse> reactions = summary.getStats();
        Integer totalReactionCount = summary.getTotalCount();

        return RecordDetailResponse.builder()
                .recordId(rec.getRecordId())                    // Long
                .gameDate(fmtDate)                              // String
                .gameTime(fmtTime)                              // String
                .emotionCode(rec.getEmotionCode())              // Integer
                .emotionLabel(emoLabel)                         // String
                .homeTeam(convertHomeTeam(game.getHomeTeam()))  // String
                .awayTeam(convertAwayTeam(game.getAwayTeam()))  // String
                .stadium(convertStadium(game.getStadium()))     // String
                .seatInfo(rec.getSeatInfo())                    // String
                .homeScore(game.getHomeScore())                 // Integer
                .awayScore(game.getAwayScore())                 // Integer
                .result(rec.getResult())                        // String
                .comment(rec.getComment())                      // String
                .longContent(rec.getLongContent())              // String
                .bestPlayer(rec.getBestPlayer())                // String
                .companions(companionDetails)                // List<String>
                .foodTags(rec.getFoodTags())                    // List<String>
                .mediaUrls(rec.getMediaUrls())
                .createdAt(createdAtStr)
                .reactions(reactions)
                .totalReactionCount(totalReactionCount)
                .build();
    }

    // 3) 마이페이지 피드 조회
    // getUserRecordsFeed에서 getById 문제 수정
    @Transactional(readOnly = true)
    public List<RecordFeedResponse> getUserRecordsFeed(Long userId) {
        return recordRepo.findByUserId(userId).stream()
                .filter(r->r.getMediaUrls()!=null && !r.getMediaUrls().isEmpty())
                .map(r->{
                    // getById 대신 findById 사용
                    Game g = gameRepo.findById(r.getGameId()).orElseThrow();
                    return new RecordFeedResponse(
                            r.getRecordId(),
                            g.getDate().format(FEED_FMT),
                            //r.getMediaUrls().get(0)
                            // 첫 번째 이미지 URL만 반환 (없으면 null)
                            r.getMediaUrls() != null && !r.getMediaUrls().isEmpty()
                                    ? r.getMediaUrls().get(0)
                                    : null
                    );
                }).toList();
    }


    // 4) 마이페이지 리스트 조회 - 수정
    @Transactional(readOnly = true)
    public List<RecordListResponse> getUserRecordsList(Long userId) {
        return recordRepo.findByUserId(userId).stream()
                .map(r -> {
                    Game g = gameRepo.findById(r.getGameId()).orElseThrow();
                    User user = userRepo.findById(r.getUserId()).orElseThrow();

                    RecordReactionSummary summary =
                            reactionService.getSummary(r.getRecordId());
                    List<ReactionStatsResponse> reactions = summary.getStats();
                    Integer totalReactionCount = summary.getTotalCount();

                    return new RecordListResponse(
                            user.getId(),
                            user.getNickname(),
                            user.getProfileImageUrl(),
                            user.getFavTeam(),
                            r.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            g.getDate().format(UPLOAD_FMT),
                            g.getTime().format(TIME_FMT),
                            convertHomeTeam(g.getHomeTeam()),
                            convertAwayTeam(g.getAwayTeam()),
                            g.getHomeScore(),
                            g.getAwayScore(),
                            convertStadium(r.getStadium()),
                            r.getEmotionCode(),
                            convertEmotionLabel(r.getEmotionCode()),
                            r.getLongContent(),
                            r.getMediaUrls(),
                            reactions,
                            totalReactionCount,
                            r.getRecordId()
                    );
                })
                .collect(Collectors.toList());
    }

    // 5) 마이페이지 캘린더 조회
    @Transactional(readOnly = true)
    public List<RecordCalendarResponse> getUserRecordsCalendar(Long userId) {
        return recordRepo.findByUserId(userId).stream()
                .map(r -> {
                    Game g = gameRepo.findById(r.getGameId()).orElseThrow();
                    return new RecordCalendarResponse(
                            g.getDate().toString(), // 프론트에서 ISO 포맷 처리
                            r.getResult()
                    );
                })
                .collect(Collectors.toList());
    }

    //레코드 삭제
    @Transactional
    public void deleteRecord(Long currentUserId, Long recordId) {
        Record record = recordRepo.findById(recordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 레코드"));
        if (!record.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "기록을 수정할 권한이 없습니다.");
        }
        recordRepo.delete(record);
    }

    // ——— Helpers ———

    private String calculateResult(String favTeam, Game game) {
        String shortFav = convertFavTeam(favTeam);
        boolean isHome = shortFav.equals(game.getHomeTeam());
        boolean isAway = shortFav.equals(game.getAwayTeam());

        // favTeam이 홈/어웨이 둘 다 아니면 "ETC"로 처리
        if (!isHome && !isAway) return "ETC"; // 또는 "기타"

        int home = game.getHomeScore() == null ? 0 : game.getHomeScore();
        int away = game.getAwayScore() == null ? 0 : game.getAwayScore();
        if (home == away) return "DRAW";
        boolean win = (isHome && home > away) || (isAway && away > home);
        return win ? "WIN" : "LOSE";
    }

    private String convertEmotionLabel(int code) {
        return switch(code) {
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

    private String convertFavTeam(String fav) {
        return switch(fav) {
            case "KIA 타이거즈" -> "KIA";
            case "두산 베어스"   -> "두산";
            case "롯데 자이언츠" -> "롯데";
            case "삼성 라이온즈" -> "삼성";
            case "키움 히어로즈" -> "키움";
            case "한화 이글스"   -> "한화";
            case "KT WIZ"      -> "KT";
            case "LG 트윈스"    -> "LG";
            case "NC 다이노스"   -> "NC";
            case "SSG 랜더스"   -> "SSG";
            default -> fav;
        };
    }

    private String convertHomeTeam(String t) {
        return switch(t) {
            case "KIA" -> "KIA 타이거즈";
            case "두산"-> "두산 베어스";
            case "롯데"-> "롯데 자이언츠";
            case "삼성"-> "삼성 라이온즈";
            case "키움"-> "키움 히어로즈";
            case "한화"-> "한화 이글스";
            case "KT"  -> "KT WIZ";
            case "LG"  -> "LG 트윈스";
            case "NC"  -> "NC 다이노스";
            case "SSG" -> "SSG 랜더스";
            default -> t;
        };
    }

    private String convertAwayTeam(String t) {
        return convertHomeTeam(t);
    }

    private String convertStadium(String s) {
        return switch(s) {
            case "잠실"   -> "잠실야구장";
            case "문학"   -> "문학야구장";
            case "고척"   -> "고척 SKYDOME";
            case "사직"   -> "사직야구장";
            case "수원"   -> "KT 위즈 파크";
            case "대전(신)"-> "한화생명 이글스 파크";
            case "대구"   -> "대구삼성라이온즈파크";
            case "광주"   -> "기아 챔피언스 필드";
            case "창원"   -> "NC 파크";
            default -> s;
        };
    }
    /**
     * 나와 맞팔(상호 팔로우)인 유저 중 닉네임으로 검색
     */
    @Transactional(readOnly = true)
    public List<UserDto> getMutualFriends(Long userId, String query) {
        // 1. 내가 팔로우하는 사람들의 ID 목록
        List<Long> followingIds =userflRepo.findByFollowerId_Id(userId).stream()
                .map(follow -> follow.getFolloweeId().getId())
                .collect(Collectors.toList());

        // 2. 나를 팔로우하는 사람들 중에서, 내가 팔로우하는 사람(1번 목록)만 필터링 -> 맞팔 관계
        Stream<User> mutualFriendsStream = userflRepo.findByFolloweeId_Id(userId).stream()
                .map(follow -> follow.getFollowerId()) // 나를 팔로우하는 User 엔티티
                .filter(follower -> followingIds.contains(follower.getId())); // 그 중에서 내가 팔로우하는 사람

        // 3. 검색어(query)가 있으면 닉네임으로 추가 필터링
        if (query != null && !query.trim().isEmpty()) {
            mutualFriendsStream = mutualFriendsStream
                    .filter(user -> user.getNickname().toLowerCase().contains(query.toLowerCase()));
        }

        // 4. DTO로 변환하여 반환
        return mutualFriendsStream
                .map(user -> new UserDto(user.getId(), user.getNickname(), user.getProfileImageUrl(), user.getFavTeam()))
                .collect(Collectors.toList());
    }

}
