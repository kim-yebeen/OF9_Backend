package com.nine.baseballdiary.backend.feed;

import com.nine.baseballdiary.backend.comment.RecordCommentRepository;
import com.nine.baseballdiary.backend.game.Game;
import com.nine.baseballdiary.backend.game.GameRepository;
import com.nine.baseballdiary.backend.like.RecordLikeRepository;
import com.nine.baseballdiary.backend.record.GameRecord;
import com.nine.baseballdiary.backend.record.GameRecordRepository;
import com.nine.baseballdiary.backend.search.dto.FollowStatus;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.repository.UserFollowRepository;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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

    /**
     * 전체 피드 조회 (최신순, 팀 필터링 지원)
     */
    @Transactional(readOnly = true)
    public List<FeedResponse> getAllFeed(FeedRequest request) {
        List<Long> followingIds = userFollowRepo.findFollowingIds(request.getUserId());
        String teamFilter = parseTeam(request.getTeam());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        // 차단 필터 포함 버전 사용
        List<GameRecord> records = recordRepo.findAllFeedRecordsWithBlockFilter(
                request.getUserId(),
                followingIds,
                teamFilter,
                pageable
        );

        return records.stream()
                .map(record -> convertToFeedResponse(record, request.getUserId()))
                .collect(Collectors.toList());
    }

    /**
     * 팔로잉 피드 조회 (최신순, 팀 필터링 지원)
     */
    @Transactional(readOnly = true)
    public List<FeedResponse> getFollowingFeed(FeedRequest request) {
        List<Long> followingIds = userFollowRepo.findFollowingIds(request.getUserId());

        if (followingIds.isEmpty()) {
            return List.of();
        }

        String teamFilter = parseTeam(request.getTeam());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        // 차단 필터 포함 버전 사용
        List<GameRecord> records = recordRepo.findFollowingFeedRecordsWithBlockFilter(
                followingIds,
                request.getUserId(),
                teamFilter,
                pageable
        );

        return records.stream()
                .map(record -> convertToFeedResponse(record, request.getUserId()))
                .collect(Collectors.toList());
    }

    private String parseTeam(String team) {
        return (team != null && !team.trim().isEmpty()) ? team.trim() : null;
    }

    private FeedResponse convertToFeedResponse(GameRecord record, Long currentUserId) {
        User user = userRepo.findById(record.getUserId()).orElseThrow();
        Game game = gameRepo.findById(record.getGame().getGameId()).orElseThrow();

        // 좋아요 정보
        long likeCount = likeRepo.countByRecordId(record.getRecordId());
        boolean isLiked = likeRepo.existsByRecordIdAndUserId(record.getRecordId(), currentUserId);

        // 댓글 개수
        long commentCount = commentRepo.countByRecordIdAndDeletedAtIsNull(record.getRecordId());

        List<String> mediaUrls = record.getMediaUrls() != null ? record.getMediaUrls() : List.of();

        return FeedResponse.builder()
                .recordId(record.getRecordId())
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .favTeam(user.getFavTeam())
                .createdAt(record.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .gameDate(game.getDate().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E)요일")))
                .gameTime(game.getTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .homeTeam(game.getHomeTeam())
                .awayTeam(game.getAwayTeam())
                .homeScore(game.getHomeScore())
                .awayScore(game.getAwayScore())
                .stadium(record.getStadium())
                .emotionCode(record.getEmotionCode())
                .emotionLabel(getEmotionLabel(record.getEmotionCode()))
                .longContent(record.getLongContent())
                .mediaUrls(mediaUrls)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .commentCount(commentCount)
                .build();
    }

    //특정 사용자의 피드 조회
    @Transactional(readOnly = true)
    public UserFeedResponse getUserFeed(Long currentUserId, Long targetUserId) {
        // 사용자 정보 조회
        User targetUser = userRepo.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));

        // 팔로우 상태 확인
        FollowStatus followStatus = getFollowStatus(currentUserId, targetUserId);

        // 사용자 통계
        long recordCount = recordRepo.countByUserId(targetUserId);
        long followerCount = userFollowRepo.countByFollowee_Id(targetUserId);
        long followingCount = userFollowRepo.countByFollower_Id(targetUserId);

        // 사용자 기록들 (피드 형식)
        List<GameRecord> records = recordRepo.findByUserIdWithDetails(targetUserId);

        List<UserFeedItem> feedItems = records.stream()
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

        return UserFeedResponse.builder()
                .userId(targetUser.getId())
                .nickname(targetUser.getNickname())
                .profileImageUrl(targetUser.getProfileImageUrl())
                .favTeam(targetUser.getFavTeam())
                .isPrivate(targetUser.getIsPrivate())
                .followStatus(followStatus)
                .recordCount(recordCount)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .feedItems(feedItems)
                .build();
    }

    private FollowStatus getFollowStatus(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            return FollowStatus.ME;
        }

        // 이미 팔로잉 중인지 확인
        boolean isFollowing = userFollowRepo.existsByFollower_IdAndFollowee_Id(currentUserId, targetUserId);
        if (isFollowing) {
            return FollowStatus.FOLLOWING;
        }

        // 팔로우 요청 대기 중인지 확인 (필요시 추가)
        // boolean isPending = followRequestRepo.existsByRequester_IdAndTarget_IdAndStatus(
        //         currentUserId, targetUserId, FollowRequestStatus.PENDING);
        // if (isPending) {
        //     return FollowStatus.REQUESTED;
        // }

        return FollowStatus.NOT_FOLLOWING;
    }

    private String getEmotionLabel(Integer emotionCode) {
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
}
