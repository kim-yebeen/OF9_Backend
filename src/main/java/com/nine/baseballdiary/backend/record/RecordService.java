package com.nine.baseballdiary.backend.record;

import com.nine.baseballdiary.backend.Notifiation.NotificationService;
import com.nine.baseballdiary.backend.comment.RecordCommentRepository;
import com.nine.baseballdiary.backend.game.Game;
import com.nine.baseballdiary.backend.game.GameRepository;
import com.nine.baseballdiary.backend.like.RecordLikeRepository;
import com.nine.baseballdiary.backend.report.service.BadgeService;
import com.nine.baseballdiary.backend.user.dto.UserDto;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.repository.UserFollowRepository;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RecordService {
    private final GameRecordRepository recordRepo;
    private final GameRepository gameRepo;
    private final UserRepository userRepo;
    private final UserFollowRepository userflRepo;
    private final RecordLikeRepository likeRepo;
    private final RecordCommentRepository commentRepo;
    private final NotificationService notificationService;
    private final BadgeService badgeService;

    // 피드, 리스트에서 짧게 보여줄 때 — "25/04/29 Fri"
    private static final DateTimeFormatter FEED_FMT =
            DateTimeFormatter.ofPattern("yy/MM/dd EEE", Locale.ENGLISH);

    // 업로드 후 상세에 "2025년 04월 29일 (금)요일" 처럼 보여줄 때
    private static final DateTimeFormatter UPLOAD_FMT =
            DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E)요일", Locale.KOREAN);

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("H:mm");

    public RecordUploadResponse uploadRecord(Long userId, CreateRecordRequest req) {
        if (req.getCompanions() != null && !req.getCompanions().isEmpty()) {
            validateCompanions(req.getCompanions());
        }

        Game game = gameRepo.findById(req.getGameId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게임: " + req.getGameId()));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저: " + userId));

        String result = calculateResult(user.getFavTeam(), game);

        GameRecord record = GameRecord.builder()
                .userId(userId)
                .game(game)
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

        GameRecord savedRecord = recordRepo.save(record);

        long totalRecords = recordRepo.countByUserId(userId); //
        boolean isFirst = (totalRecords == 1);

        notificationService.createNewRecordNotification(userId, savedRecord.getRecordId());
        badgeService.checkAndAwardBadgesForUser(userId);

        String dateStr = game.getDate().format(UPLOAD_FMT);
        return new RecordUploadResponse(savedRecord.getRecordId(), dateStr, isFirst);
    }

    private void validateCompanions(List<Long> companionIds) {
        // 'findAllById'는 Spring Data JPA의 기본 CrudRepository 메소드입니다.
        List<User> foundUsers = userRepo.findAllById(companionIds);

        // 요청한 ID 목록의 크기와 실제 DB에서 찾은 사용자 목록의 크기가 다르면,
        // 존재하지 않는 사용자가 포함된 것입니다.
        if (foundUsers.size() != companionIds.size()) {
            throw new IllegalArgumentException("태그한 사용자 중 존재하지 않는 사용자가 있습니다.");
        }
    }

    @Transactional
    public RecordDetailResponse updateRecord(Long currentUserId, Long recordId, UpdateRecordRequest req) {
        GameRecord rec = recordRepo.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 레코드"));
        if (!rec.getUserId().equals(currentUserId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "기록을 수정할 권한이 없습니다.");
        if (req.getCompanions() != null && !req.getCompanions().isEmpty()) {
            validateCompanions(req.getCompanions());
        }
        rec.setComment(req.getComment());
        rec.setLongContent(req.getLongContent());
        rec.setBestPlayer(req.getBestPlayer());
        rec.setCompanions(req.getCompanions());
        rec.setFoodTags(req.getFoodTags());
        rec.setMediaUrls(req.getMediaUrls());
        recordRepo.save(rec);
        return getRecordDetail(recordId);
    }

    @Transactional(readOnly = true)
    public RecordDetailResponse getRecordDetail(Long recordId) {
        GameRecord rec = recordRepo.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 레코드 ID: " + recordId));

        // 작성자 정보 조회 추가
        User author = userRepo.findById(rec.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 작성자: " + rec.getUserId()));

        Game game = rec.getGame();
        if (game == null) {
            throw new IllegalArgumentException("존재하지 않는 게임 ID 참조: " + rec.getRecordId());
        }

        List<Long> companionIds = rec.getCompanions();
        List<UserDto> companionDetails = List.of();

        if (companionIds != null && !companionIds.isEmpty()) {
            companionDetails = userRepo.findAllById(companionIds).stream()
                    .map(UserDto::from)
                    .collect(Collectors.toList());
        }

        String fmtDate = game.getDate().format(UPLOAD_FMT);
        String fmtTime = game.getTime().format(TIME_FMT);
        String emoLabel = convertEmotionLabel(rec.getEmotionCode());
        String createdAtStr = rec.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // ✅ 좋아요 및 댓글 정보 조회
        long likeCount = likeRepo.countByRecordId(recordId);
        long commentCount = commentRepo.countByRecordIdAndDeletedAtIsNull(recordId);

        return RecordDetailResponse.builder()
                .recordId(rec.getRecordId())
                .userId(author.getId())
                .nickname(author.getNickname())
                .profileImageUrl(author.getProfileImageUrl())
                .favTeam(author.getFavTeam())
                .gameDate(fmtDate)
                .gameTime(fmtTime)
                .emotionCode(rec.getEmotionCode())
                .emotionLabel(emoLabel)
                .homeTeam(convertHomeTeam(game.getHomeTeam()))
                .awayTeam(convertAwayTeam(game.getAwayTeam()))
                .stadium(convertStadium(game.getStadium()))
                .seatInfo(rec.getSeatInfo())
                .homeScore(game.getHomeScore())
                .awayScore(game.getAwayScore())
                .result(rec.getResult())
                .comment(rec.getComment())
                .longContent(rec.getLongContent())
                .bestPlayer(rec.getBestPlayer())
                .companions(companionDetails)
                .foodTags(rec.getFoodTags())
                .mediaUrls(rec.getMediaUrls())
                .createdAt(createdAtStr)
                .likeCount(likeCount)
                .isLiked(false)  // currentUserId 없으면 false
                .commentCount(commentCount)
                .gameDate(fmtDate)
                .gameTime(fmtTime)
                .build();
    }

    @Transactional(readOnly = true)
    public RecordDetailResponse getRecordDetailWithUser(Long recordId, Long currentUserId) {
        GameRecord rec = recordRepo.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 레코드 ID: " + recordId));
        Game game = rec.getGame();
        if (game == null) {
            throw new IllegalArgumentException("존재하지 않는 게임 ID 참조: " + rec.getRecordId());
        }

        User author = userRepo.findById(rec.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 작성자: " + rec.getUserId()));

        List<Long> companionIds = rec.getCompanions();
        List<UserDto> companionDetails = List.of();

        if (companionIds != null && !companionIds.isEmpty()) {
            companionDetails = userRepo.findAllById(companionIds).stream()
                    .map(UserDto::from)
                    .collect(Collectors.toList());
        }

        String fmtDate = game.getDate().format(UPLOAD_FMT);
        String fmtTime = game.getTime().format(TIME_FMT);
        String emoLabel = convertEmotionLabel(rec.getEmotionCode());
        String createdAtStr = rec.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // ✅ 좋아요 및 댓글 정보 조회 (currentUserId 포함)
        long likeCount = likeRepo.countByRecordId(recordId);
        boolean isLiked = likeRepo.existsByRecordIdAndUserId(recordId, currentUserId);
        long commentCount = commentRepo.countByRecordIdAndDeletedAtIsNull(recordId);

        return RecordDetailResponse.builder()
                .recordId(rec.getRecordId())
                .userId(author.getId())
                .nickname(author.getNickname())
                .profileImageUrl(author.getProfileImageUrl())
                .favTeam(author.getFavTeam())
                .gameDate(fmtDate)
                .gameTime(fmtTime)
                .emotionCode(rec.getEmotionCode())
                .emotionLabel(emoLabel)
                .homeTeam(convertHomeTeam(game.getHomeTeam()))
                .awayTeam(convertAwayTeam(game.getAwayTeam()))
                .stadium(convertStadium(game.getStadium()))
                .seatInfo(rec.getSeatInfo())
                .homeScore(game.getHomeScore())
                .awayScore(game.getAwayScore())
                .result(rec.getResult())
                .comment(rec.getComment())
                .longContent(rec.getLongContent())
                .bestPlayer(rec.getBestPlayer())
                .companions(companionDetails)
                .foodTags(rec.getFoodTags())
                .mediaUrls(rec.getMediaUrls())
                .createdAt(createdAtStr)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .commentCount(commentCount)
                .gameDate(fmtDate)
                .gameTime(fmtTime)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RecordFeedResponse> getUserRecordsFeed(Long userId) {
        List<GameRecord> records = recordRepo.findByUserIdWithDetails(userId);

        return records.stream()
                .filter(r -> r.getMediaUrls() != null && !r.getMediaUrls().isEmpty())
                .map(r -> {
                    // 좋아요 개수 조회
                    long likeCount = likeRepo.countByRecordId(r.getRecordId());

                    return new RecordFeedResponse(
                            r.getRecordId(),
                            r.getGame().getDate().format(FEED_FMT),
                            List.of(r.getMediaUrls().get(0)),
                            likeCount
                    );
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecordListResponse> getUserRecordsList(Long targetUserId, Long currentUserId) {
        // userId -> targetUserId로 변경
        List<GameRecord> records = recordRepo.findByUserIdWithDetails(targetUserId);

        User user = userRepo.findById(targetUserId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 사용자: " + targetUserId)
        );

        return records.stream()
                .map(r -> {
                    Game g = r.getGame();

                    // ✅ 좋아요 및 댓글 정보 조회
                    long likeCount = likeRepo.countByRecordId(r.getRecordId());
                    boolean isLiked = likeRepo.existsByRecordIdAndUserId(r.getRecordId(), currentUserId);
                    long commentCount = commentRepo.countByRecordIdAndDeletedAtIsNull(r.getRecordId());

                    return new RecordListResponse(
                            user.getId(), user.getNickname(), user.getProfileImageUrl(), user.getFavTeam(),
                            r.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            g.getDate().format(UPLOAD_FMT),
                            g.getTime() != null ? g.getTime().format(TIME_FMT) : "",
                            convertHomeTeam(g.getHomeTeam()), convertAwayTeam(g.getAwayTeam()),
                            g.getHomeScore(), g.getAwayScore(),
                            convertStadium(r.getStadium()),
                            r.getEmotionCode(), convertEmotionLabel(r.getEmotionCode()),
                            r.getLongContent(), r.getMediaUrls(),
                            likeCount, isLiked, commentCount, r.getRecordId()
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserRecordsCalendar(Long targetUserId, int year, int month) {    // 해당 월의 시작일과 종료일
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        // 해당 월의 기록들 조회
        List<GameRecord> monthRecords = recordRepo.findByUserIdAndGameDateBetween(targetUserId, startDate, endDate);

        // 개별 기록들 (기존 방식)
        List<RecordCalendarResponse> records = monthRecords.stream()
                .map(r -> {
                    Game g = r.getGame();
                    return new RecordCalendarResponse(
                            g.getDate().toString(),
                            r.getResult()
                    );
                })
                .collect(Collectors.toList());

        // 월별 통계 계산
        long wins = monthRecords.stream().filter(r -> "WIN".equals(r.getResult())).count();
        long losses = monthRecords.stream().filter(r -> "LOSE".equals(r.getResult())).count();
        double winRate = (wins + losses == 0) ? 0.0 : Math.round(((double) wins / (wins + losses)) * 1000.0) / 10.0;

        int recordCount = monthRecords.size();

        long totalLikes = monthRecords.stream()
                .mapToLong(r -> likeRepo.countByRecordId(r.getRecordId()))
                .sum();

        // 월별 통계
        Map<String, Object> monthlyStats = Map.of(
                "winRate", winRate,
                "recordCount", recordCount,
                "totalLikes", totalLikes
        );

        // 최종 응답
        return Map.of(
                "records", records,
                "monthlyStats", monthlyStats
        );
    }

    @Transactional
    public void deleteRecord(Long currentUserId, Long recordId) {
        GameRecord record = recordRepo.findById(recordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 레코드"));

        if (!record.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "기록을 수정할 권한이 없습니다.");
        }

        recordRepo.delete(record);
    }

    // Helper methods
    private String calculateResult(String favTeam, Game game) {
        String shortFav = convertFavTeam(favTeam);
        boolean isHome = shortFav.equals(game.getHomeTeam());
        boolean isAway = shortFav.equals(game.getAwayTeam());

        if (!isHome && !isAway) return "ETC";

        int home = game.getHomeScore() == null ? 0 : game.getHomeScore();
        int away = game.getAwayScore() == null ? 0 : game.getAwayScore();
        if (home == away) return "TIE";
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
            case "두산 베어스" -> "두산";
            case "롯데 자이언츠" -> "롯데";
            case "삼성 라이온즈" -> "삼성";
            case "키움 히어로즈" -> "키움";
            case "한화 이글스" -> "한화";
            case "KT WIZ" -> "KT";
            case "LG 트윈스" -> "LG";
            case "NC 다이노스" -> "NC";
            case "SSG 랜더스" -> "SSG";
            default -> fav;
        };
    }

    private String convertHomeTeam(String t) {
        return switch(t) {
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
            default -> t;
        };
    }

    private String convertAwayTeam(String t) {
        return convertHomeTeam(t);
    }

    private String convertStadium(String s) {
        return switch(s) {
            case "잠실" -> "잠실야구장";
            case "문학" -> "문학야구장";
            case "고척" -> "고척 SKYDOME";
            case "사직" -> "사직야구장";
            case "수원" -> "KT 위즈 파크";
            case "대전(신)" -> "한화생명 이글스 파크";
            case "대구" -> "대구삼성라이온즈파크";
            case "광주" -> "기아 챔피언스 필드";
            case "창원" -> "NC 파크";
            default -> s;
        };
    }

    @Transactional(readOnly = true)
    public List<UserDto> getMutualFriends(Long userId, String query) {
        // userId 파라미터는 컨트롤러와의 호환성을 위해 유지하지만, 실제 로직에서는 사용되지 않습니다.

        if (query == null || query.trim().isEmpty()) {
            // 검색어가 없으면 빈 리스트를 반환합니다.
            // (모든 사용자를 반환하는 것은 성능 및 UX에 좋지 않습니다.)
            return List.of();
        }
        List<User> allUsers = userRepo.findAll();

        Stream<User> filteredUsers = allUsers.stream()
                .filter(user -> user.getNickname().toLowerCase().contains(query.toLowerCase()));

        return filteredUsers
                .map(UserDto::from)
                .collect(Collectors.toList());
    }
}