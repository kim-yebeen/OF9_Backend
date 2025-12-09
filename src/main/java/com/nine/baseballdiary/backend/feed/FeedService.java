package com.nine.baseballdiary.backend.feed;

import com.nine.baseballdiary.backend.comment.RecordCommentRepository;
import com.nine.baseballdiary.backend.game.Game;
import com.nine.baseballdiary.backend.game.GameRepository;
import com.nine.baseballdiary.backend.like.RecordLikeRepository;
import com.nine.baseballdiary.backend.record.GameRecord;
import com.nine.baseballdiary.backend.record.GameRecordRepository;
import com.nine.baseballdiary.backend.record.RecordListResponse;
import com.nine.baseballdiary.backend.record.RecordService;
import com.nine.baseballdiary.backend.search.dto.FollowStatus;
import com.nine.baseballdiary.backend.user.entity.FollowRequestStatus;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.repository.FollowRequestRepository;
import com.nine.baseballdiary.backend.user.repository.UserBlockRepository;
import com.nine.baseballdiary.backend.user.repository.UserFollowRepository;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedService {

    private final GameRecordRepository recordRepo;
    private final UserFollowRepository userFollowRepo;
    private final GameRepository gameRepo;
    private final UserRepository userRepo;
    private final RecordLikeRepository likeRepo;
    private final RecordCommentRepository commentRepo;
    private final FollowRequestRepository followRequestRepo;
    private final RecordService recordService;
    private final UserBlockRepository userBlockRepo;

    /**
     * 전체 피드 조회 (최신순, 필터링 적용)
     */
    @Transactional(readOnly = true)
    public List<FeedResponse> getAllFeed(FeedRequest request) {
        List<Long> followingIds = userFollowRepo.findFollowingIds(request.getUserId());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        String teamFilter = parseString(request.getTeam());

        // [수정] 구장 필터를 LIKE 검색으로 변경
        String rawStadium = parseString(request.getStadium());
        String stadiumFilter = null;
        if (rawStadium != null) {
            // "대전"으로 검색하면 "대전(신)", "대전 한화생명볼파크" 모두 매칭
            stadiumFilter = "%" + rawStadium + "%";
        }

        String seatFilter = parseString(request.getSeatInfo());
        if (seatFilter != null) {
            seatFilter = "%" + seatFilter + "%";
        }

        LocalDate targetDate = parseDate(request.getDate());

        List<GameRecord> records = recordRepo.findAllFeedRecordsWithFilters(
                request.getUserId(),
                followingIds,
                teamFilter,
                stadiumFilter, // LIKE 검색용 패턴
                seatFilter,
                targetDate,
                pageable
        );

        return records.stream()
                .map(record -> convertToFeedResponse(record, request.getUserId()))
                .collect(Collectors.toList());
    }

    /**
     * 팔로잉 피드 조회 (최신순, 필터링 적용)
     */
    @Transactional(readOnly = true)
    public List<FeedResponse> getFollowingFeed(FeedRequest request) {
        List<Long> followingIds = userFollowRepo.findFollowingIds(request.getUserId());

        if (followingIds.isEmpty()) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        String teamFilter = parseString(request.getTeam());

        // ✅ getAllFeed()처럼 LIKE 패턴으로 변경
        String rawStadium = parseString(request.getStadium());
        String stadiumFilter = null;
        if (rawStadium != null) {
            stadiumFilter = "%" + rawStadium + "%";  // LIKE 검색용 패턴
        }

        String seatFilter = parseString(request.getSeatInfo());
        if (seatFilter != null) {
            seatFilter = "%" + seatFilter + "%";
        }

        LocalDate targetDate = parseDate(request.getDate());

        List<GameRecord> records = recordRepo.findFollowingFeedRecordsWithFilters(
                followingIds,
                request.getUserId(),
                teamFilter,
                stadiumFilter,  // LIKE 패턴
                seatFilter,
                targetDate,
                pageable
        );

        return records.stream()
                .map(record -> convertToFeedResponse(record, request.getUserId()))
                .collect(Collectors.toList());
    }

    // 빈 문자열이나 공백을 null로 변환
    private String parseString(String value) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
    }

    // 날짜 문자열(YYYY-MM-DD)을 LocalDate로 변환
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private FeedResponse convertToFeedResponse(GameRecord record, Long currentUserId) {
        User user = userRepo.findById(record.getUserId()).orElseThrow();
        Game game = gameRepo.findById(record.getGame().getGameId()).orElseThrow();

        long likeCount = likeRepo.countByRecordId(record.getRecordId());
        boolean isLiked = likeRepo.existsByRecordIdAndUserId(record.getRecordId(), currentUserId);
        long commentCount = commentRepo.countByRecordIdAndDeletedAtIsNull(record.getRecordId());
        List<String> mediaUrls = record.getMediaUrls() != null ? record.getMediaUrls() : List.of();

        // ✅ followStatus 계산
        FollowStatus followStatus = getFollowStatus(currentUserId, user.getId());

        // ✅ isMutualFollow 계산
        Boolean isMutualFollow = false;
        if (followStatus == FollowStatus.NOT_FOLLOWING) {
            // 내가 팔로우하지 않는 상태에서, 상대방이 나를 팔로우하고 있는지 확인
            boolean isFollower = userFollowRepo.existsByFollower_IdAndFollowee_Id(user.getId(), currentUserId);
            isMutualFollow = isFollower;
        }

        return FeedResponse.builder()
                .recordId(record.getRecordId())
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .favTeam(user.getFavTeam())
                .followStatus(followStatus)      // ✅ 추가
                .isMutualFollow(isMutualFollow)  // ✅ 추가
                .createdAt(record.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .gameDate(game.getDate().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E)요일")))
                .gameTime(game.getTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .homeTeam(game.getHomeTeam())
                .awayTeam(game.getAwayTeam())
                .homeScore(game.getHomeScore())
                .awayScore(game.getAwayScore())
                .stadium(recordService.convertStadium(record.getStadium()))
                .emotionCode(record.getEmotionCode())
                .emotionLabel(getEmotionLabel(record.getEmotionCode()))
                .longContent(record.getLongContent())
                .mediaUrls(mediaUrls)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .commentCount(commentCount)
                .build();
    }

    @Transactional(readOnly = true)
    public UserFeedResponse getUserFeed(Long currentUserId, Long targetUserId) {
        User targetUser = userRepo.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));

        FollowStatus followStatus = getFollowStatus(currentUserId, targetUserId);

        // ✅ isMutualFollow 계산
        Boolean isMutualFollow = false;
        if (followStatus == FollowStatus.NOT_FOLLOWING) {
            boolean isFollower = userFollowRepo.existsByFollower_IdAndFollowee_Id(targetUserId, currentUserId);
            isMutualFollow = isFollower;
        }

        long recordCount = recordRepo.countByUserId(targetUserId);
        long followerCount = userFollowRepo.countByFollowee_Id(targetUserId);
        long followingCount = userFollowRepo.countByFollower_Id(targetUserId);

        boolean isBlocked = userBlockRepo.existsByBlocker_IdAndBlocked_Id(currentUserId, targetUserId) ||
                userBlockRepo.existsByBlocker_IdAndBlocked_Id(targetUserId, currentUserId);

        boolean canViewContent = !isBlocked && (!targetUser.getIsPrivate() ||
                followStatus == FollowStatus.ME ||
                followStatus == FollowStatus.FOLLOWING);

        List<UserFeedItem> feedItems = List.of();
        if (canViewContent) {
            List<GameRecord> records = recordRepo.findByUserIdWithDetails(targetUserId);
            feedItems = records.stream()
                    .filter(r -> r.getMediaUrls() != null && !r.getMediaUrls().isEmpty())
                    .map(r -> {
                        long likeCount = likeRepo.countByRecordId(r.getRecordId());
                        return UserFeedItem.builder()
                                .recordId(r.getRecordId())
                                .gameDate(r.getGame().getDate().format(DateTimeFormatter.ofPattern("yy/MM/dd EEE", Locale.ENGLISH)))
                                .imageUrl(r.getMediaUrls().get(0))
                                .likeCount(likeCount)
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        return UserFeedResponse.builder()
                .userId(targetUser.getId())
                .nickname(targetUser.getNickname())
                .profileImageUrl(targetUser.getProfileImageUrl())
                .favTeam(targetUser.getFavTeam())
                .isPrivate(targetUser.getIsPrivate())
                .followStatus(followStatus)
                .isMutualFollow(isMutualFollow)  // ✅ 추가
                .recordCount(recordCount)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .feedItems(feedItems)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RecordListResponse> getUserList(Long currentUserId, Long targetUserId) {
        User targetUser = userRepo.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));
        FollowStatus followStatus = getFollowStatus(currentUserId, targetUserId);
        boolean isBlocked = userBlockRepo.existsByBlocker_IdAndBlocked_Id(currentUserId, targetUserId) ||
                userBlockRepo.existsByBlocker_IdAndBlocked_Id(targetUserId, currentUserId);
        boolean canViewContent = !isBlocked && (!targetUser.getIsPrivate() ||
                followStatus == FollowStatus.ME ||
                followStatus == FollowStatus.FOLLOWING);
        if (!canViewContent) return List.of();
        return recordService.getUserRecordsList(targetUserId, currentUserId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserCalendar(Long currentUserId, Long targetUserId, int year, int month) {
        User targetUser = userRepo.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));
        FollowStatus followStatus = getFollowStatus(currentUserId, targetUserId);
        boolean isBlocked = userBlockRepo.existsByBlocker_IdAndBlocked_Id(currentUserId, targetUserId) ||
                userBlockRepo.existsByBlocker_IdAndBlocked_Id(targetUserId, currentUserId);
        boolean canViewContent = !isBlocked && (!targetUser.getIsPrivate() ||
                followStatus == FollowStatus.ME ||
                followStatus == FollowStatus.FOLLOWING);
        if (!canViewContent) return Map.of("records", List.of(), "monthlyStats", Map.of());
        return recordService.getUserRecordsCalendar(targetUserId, year, month);
    }

    private FollowStatus getFollowStatus(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) return FollowStatus.ME;
        boolean isFollowing = userFollowRepo.existsByFollower_IdAndFollowee_Id(currentUserId, targetUserId);
        if (isFollowing) return FollowStatus.FOLLOWING;
        boolean isPending = followRequestRepo.existsByRequester_IdAndTarget_IdAndStatus(
                currentUserId, targetUserId, FollowRequestStatus.PENDING);
        if (isPending) return FollowStatus.REQUESTED;
        return FollowStatus.NOT_FOLLOWING;
    }

    private String getEmotionLabel(Integer emotionCode) {
        if (emotionCode == null) {
            return "알 수 없음";
        }

        switch (emotionCode) {
            case 1: return "행복해요";
            case 2: return "놀랐어요";
            case 3: return "짜릿해요";
            case 4: return "벅차요";
            case 5: return "통쾌해요";
            case 6: return "만족해요";
            case 7: return "지루해요";
            case 8: return "무난해요";
            case 9: return "긴장돼요";
            case 10: return "질투나요";
            case 11: return "답답해요";
            case 12: return "아쉬워요";
            case 13: return "지쳤어요";
            case 14: return "허탈해요";
            case 15: return "짜증나요";
            case 16: return "화나요";
            default: return "알 수 없음";
        }
    }
}