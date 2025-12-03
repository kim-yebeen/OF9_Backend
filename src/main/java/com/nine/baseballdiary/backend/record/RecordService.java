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

        String standardizedStadium = convertStadium(req.getStadium());

        GameRecord record = GameRecord.builder()
                .userId(userId)
                .game(game)
                .stadium(standardizedStadium)
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
        // 1. 레코드 조회 (FetchType.LAZY를 고려하여 기본 findById 사용)
        GameRecord rec = recordRepo.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 레코드"));

        // 2. 권한 확인
        if (!rec.getUserId().equals(currentUserId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "기록을 수정할 권한이 없습니다.");

        // 3. "null이 아니면 수정" 로직
        if (req.getComment() != null) {rec.setComment(req.getComment());}
        if (req.getLongContent() != null) {rec.setLongContent(req.getLongContent());}
        if (req.getBestPlayer() != null) {rec.setBestPlayer(req.getBestPlayer());}
        if (req.getCompanions() != null) {
            if (!req.getCompanions().isEmpty()) { validateCompanions(req.getCompanions());}
            rec.setCompanions(req.getCompanions());
        }
        if (req.getFoodTags() != null) { rec.setFoodTags(req.getFoodTags());}
        if (req.getMediaUrls() != null) { rec.setMediaUrls(req.getMediaUrls()); }
        if (req.getStadium() != null) {
            rec.setStadium(convertStadium(req.getStadium()));
        }
        if (req.getSeatInfo() != null) { rec.setSeatInfo(req.getSeatInfo());}
        if (req.getEmotionCode() != null) {rec.setEmotionCode(req.getEmotionCode());}

        // 4. gameId가 변경되었을 때의 로직
        if (req.getGameId() != null) {
            boolean needsGameUpdate = false;
            // [null 방어 1] 기존 게임이 아예 없었는지 확인
            if (rec.getGame() == null) {
                needsGameUpdate = true;
            }
            // [null 방어 2] 기존 게임이 있었고, ID가 다른지 확인
            else if (!req.getGameId().equals(rec.getGame().getGameId())) {
                needsGameUpdate = true;
            }

            if (needsGameUpdate) {
                Game newGame = gameRepo.findById(req.getGameId())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게임: " + req.getGameId()));
                rec.setGame(newGame);

                User user = userRepo.findById(currentUserId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저: " + currentUserId));
                String newResult = calculateResult(user.getFavTeam(), newGame);
                rec.setResult(newResult);
            }
        }
        //    DB에 다시 묻지 않고, 지금 가진 'rec' 객체로 'Response'를 직접 만듭니다.

        User author = userRepo.findById(rec.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 작성자: " + rec.getUserId()));

        Game game = rec.getGame(); // 방금 업데이트한 최신 game 정보가 들어있음

        List<Long> companionIds = rec.getCompanions();
        List<UserDto> companionDetails = List.of();

        if (companionIds != null && !companionIds.isEmpty()) {
            companionDetails = userRepo.findAllById(companionIds).stream()
                    .map(UserDto::from)
                    .collect(Collectors.toList());
        }

        // game 객체 자체가 null일 때를 대비
        String fmtDate = (game != null) ? game.getDate().format(UPLOAD_FMT) : "날짜 정보 없음";
        String fmtTime = (game != null && game.getTime() != null) ? game.getTime().format(TIME_FMT) : "";
        String emoLabel = convertEmotionLabel(rec.getEmotionCode());
        String createdAtStr = rec.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        long likeCount = likeRepo.countByRecordId(recordId);
        boolean isLiked = likeRepo.existsByRecordIdAndUserId(recordId, currentUserId);
        long commentCount = commentRepo.countByRecordIdAndDeletedAtIsNull(recordId);

        // 7. 새로 만든 Response를 반환
        return RecordDetailResponse.builder()
                .recordId(rec.getRecordId())
                .userId(author.getId())
                .nickname(author.getNickname())
                .profileImageUrl(author.getProfileImageUrl())
                .favTeam(author.getFavTeam())
                .gameId(game != null ? game.getGameId() : null)    // ← [수정] 1. updateRecord
                .gameDate(fmtDate)
                .gameTime(fmtTime)
                .emotionCode(rec.getEmotionCode())
                .emotionLabel(emoLabel)
                .homeTeam(game != null ? convertHomeTeam(game.getHomeTeam()) : "팀 정보 없음")
                .awayTeam(game != null ? convertAwayTeam(game.getAwayTeam()) : "팀 정보 없음")
                .stadium(convertStadium(rec.getStadium()))
                .seatInfo(rec.getSeatInfo())
                .homeScore(game != null ? game.getHomeScore() : 0)
                .awayScore(game != null ? game.getAwayScore() : 0)
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
                .build();
    }

    @Transactional(readOnly = true)
    public RecordDetailResponse getRecordDetail(Long recordId) {
        GameRecord rec = recordRepo.findByIdWithDetails(recordId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 레코드 ID: " + recordId));

        // 작성자 정보 조회 추가
        User author = userRepo.findById(rec.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 작성자: " + rec.getUserId()));

        Game game = rec.getGame(); // game이 null일 수 있음

        List<Long> companionIds = rec.getCompanions();
        List<UserDto> companionDetails = List.of();

        if (companionIds != null && !companionIds.isEmpty()) {
            companionDetails = userRepo.findAllById(companionIds).stream()
                    .map(UserDto::from)
                    .collect(Collectors.toList());
        }

        // game이 null일 수 있으므로 null 체크
        String fmtDate = (game != null) ? game.getDate().format(UPLOAD_FMT) : "날짜 정보 없음";
        String fmtTime = (game != null && game.getTime() != null) ? game.getTime().format(TIME_FMT) : "";
        String emoLabel = convertEmotionLabel(rec.getEmotionCode());
        String createdAtStr = rec.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 좋아요 및 댓글 정보 조회
        long likeCount = likeRepo.countByRecordId(recordId);
        long commentCount = commentRepo.countByRecordIdAndDeletedAtIsNull(recordId);

        return RecordDetailResponse.builder()
                .recordId(rec.getRecordId())
                .userId(author.getId())
                .nickname(author.getNickname())
                .profileImageUrl(author.getProfileImageUrl())
                .favTeam(author.getFavTeam())
                .gameId(game != null ? game.getGameId() : null)
                .gameDate(fmtDate)
                .gameTime(fmtTime)
                .emotionCode(rec.getEmotionCode())
                .emotionLabel(emoLabel)
                .homeTeam(game != null ? convertHomeTeam(game.getHomeTeam()) : "팀 정보 없음")
                .awayTeam(game != null ? convertAwayTeam(game.getAwayTeam()) : "팀 정보 없음")
                .stadium(convertStadium(rec.getStadium()))
                .seatInfo(rec.getSeatInfo())
                .homeScore(game != null ? game.getHomeScore() : 0)
                .awayScore(game != null ? game.getAwayScore() : 0)
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
                .build();
    }

    @Transactional(readOnly = true)
    public RecordDetailResponse getRecordDetailWithUser(Long recordId, Long currentUserId) {
        GameRecord rec = recordRepo.findByIdWithDetails(recordId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 레코드 ID: " + recordId));
        Game game = rec.getGame(); // game이 null일 수 있음

        User author = userRepo.findById(rec.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 작성자: " + rec.getUserId()));

        List<Long> companionIds = rec.getCompanions();
        List<UserDto> companionDetails = List.of();

        if (companionIds != null && !companionIds.isEmpty()) {
            companionDetails = userRepo.findAllById(companionIds).stream()
                    .map(UserDto::from)
                    .collect(Collectors.toList());
        }

        // game이 null이어도 오류가 나지 않도록 함
        String fmtDate = (game != null) ? game.getDate().format(UPLOAD_FMT) : "날짜 정보 없음";
        // game.getTime()이 null일 경우 NullPointerException 방지
        String fmtTime = (game != null && game.getTime() != null) ? game.getTime().format(TIME_FMT) : "";
        String emoLabel = convertEmotionLabel(rec.getEmotionCode());
        String createdAtStr = rec.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 좋아요 및 댓글 정보 조회 (currentUserId 포함)
        long likeCount = likeRepo.countByRecordId(recordId);
        boolean isLiked = likeRepo.existsByRecordIdAndUserId(recordId, currentUserId);
        long commentCount = commentRepo.countByRecordIdAndDeletedAtIsNull(recordId);

        return RecordDetailResponse.builder()
                .recordId(rec.getRecordId())
                .userId(author.getId())
                .nickname(author.getNickname())
                .profileImageUrl(author.getProfileImageUrl())
                .favTeam(author.getFavTeam())
                .gameId(game != null ? game.getGameId() : null)
                .gameDate(fmtDate)
                .gameTime(fmtTime)
                .emotionCode(rec.getEmotionCode())
                .emotionLabel(emoLabel)
                .homeTeam(game != null ? convertHomeTeam(game.getHomeTeam()) : "팀 정보 없음")
                .awayTeam(game != null ? convertAwayTeam(game.getAwayTeam()) : "팀 정보 없음")
                .stadium(convertStadium(rec.getStadium()))
                .seatInfo(rec.getSeatInfo())
                .homeScore(game != null ? game.getHomeScore() : 0)
                .awayScore(game != null ? game.getAwayScore() : 0)
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

                    // [수정] r.getGame()이 null일 수 있으므로 방어 코드 추가
                    Game g = r.getGame();
                    String gameDate = (g != null) ? g.getDate().format(FEED_FMT) : "날짜 없음";

                    return new RecordFeedResponse(
                            r.getRecordId(),
                            gameDate, // [수정] g.getDate().format(FEED_FMT) -> gameDate
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
                    Game g = r.getGame(); // g가 null일 수 있음

                    // 좋아요 및 댓글 정보 조회
                    long likeCount = likeRepo.countByRecordId(r.getRecordId());
                    boolean isLiked = likeRepo.existsByRecordIdAndUserId(r.getRecordId(), currentUserId);
                    long commentCount = commentRepo.countByRecordIdAndDeletedAtIsNull(r.getRecordId());

                    // [수정] g가 null일 경우를 대비
                    String gameDate = (g != null) ? g.getDate().format(UPLOAD_FMT) : "날짜 정보 없음";
                    String gameTime = (g != null && g.getTime() != null) ? g.getTime().format(TIME_FMT) : "";
                    String homeTeam = (g != null) ? convertHomeTeam(g.getHomeTeam()) : "팀 정보 없음";
                    String awayTeam = (g != null) ? convertAwayTeam(g.getAwayTeam()) : "팀 정보 없음";
                    Integer homeScore = (g != null) ? g.getHomeScore() : 0;
                    Integer awayScore = (g != null) ? g.getAwayScore() : 0;


                    return new RecordListResponse(
                            user.getId(), user.getNickname(), user.getProfileImageUrl(), user.getFavTeam(),
                            r.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            gameDate,
                            gameTime,
                            homeTeam,
                            awayTeam,
                            homeScore,
                            awayScore,
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
                    Game g = r.getGame(); // g가 null일 수 있음

                    // [수정] g가 null일 경우 대비
                    String gameDate = (g != null) ? g.getDate().toString() : "날짜 없음";

                    return new RecordCalendarResponse(
                            gameDate, // [수정] g.getDate().toString() -> gameDate
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

       private String calculateResult(String favTeam, Game game) {
        // [수정] game이 null이면 계산 불가
        if (game == null) {
            return "ETC";
        }

        String shortFav = convertFavTeam(favTeam); // favTeam이 null이어도 괜찮도록 아래에서 수정
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

    // [!!! 수정된 convertFavTeam 메소드 (null 방어) !!!]
    private String convertFavTeam(String fav) {
        // [수정] fav가 null일 경우 NullPointerException 방지
        if (fav == null) {
            return ""; // 또는 "알 수 없음"
        }

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
        // [수정] t가 null일 경우 대비
        if (t == null) return "알 수 없음";
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

    public String convertStadium(String s) {
        if (s == null) {
            return "알 수 없음";
        }

        // 공백 제거 후 비교
        String normalized = s.replace(" ", "");

        return switch(normalized) {
            case "잠실", "잠실야구장" -> "잠실 야구장";
            case "사직", "사직야구장" -> "사직 야구장";
            case "대구", "대구삼성라이온즈파크" -> "대구삼성라이온즈파크";
            case "고척", "고척SKYDOME" -> "고척 SKYDOME";
            case "대전", "대전(신)", "한화생명볼파크", "한화생명이글스파크",
                    "대전한화생명이글스파크", "대전한화생명볼파크" -> "한화생명 볼파크";
            case "광주", "기아챔피언스필드", "광주-기아챔피언스필드" -> "기아 챔피언스 필드";
            case "수원", "수원케이티위즈파크", "KT위즈파크" -> "수원 케이티 위즈 파크";
            case "창원", "창원NC파크", "NC파크" -> "창원 NC 파크";
            case "문학", "인천SSG랜더스필드", "SSG랜더스필드" -> "인천 SSG 랜더스필드";
            default -> s; // 변환 불가능하면 원본 반환
        };
    }

    @Transactional(readOnly = true)
    public List<UserDto> getMutualFriends(Long userId, String query) {
        // userId 파라미터는 컨트롤러와의 호환성을 위해 유지합니다.

        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        // [수정] findAll() -> findByNicknameContainingIgnoreCase()
        List<User> users = userRepo.findByNicknameContainingIgnoreCase(query);

        // [수정] 자기 자신(userId)을 필터로 제외합니다.
        return users.stream()
                .filter(user -> !user.getId().equals(userId))
                .map(UserDto::from)
                .collect(Collectors.toList());
    }
}