package com.nine.baseballdiary.backend.feed;

import com.nine.baseballdiary.backend.reaction.TopReactionsResponse;
import com.nine.baseballdiary.backend.record.Record;
import com.nine.baseballdiary.backend.record.RecordRepository;
import com.nine.baseballdiary.backend.game.Game;
import com.nine.baseballdiary.backend.game.GameRepository;
import com.nine.baseballdiary.backend.reaction.ReactionService;

import com.nine.baseballdiary.backend.user.entity.User;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedService {

    private final RecordRepository recordRepo;
    private final UserFollowRepository userFollowRepo;
    private final ReactionService reactionService;
    private final GameRepository gameRepo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public List<FeedResponse> getAllFeed(FeedRequest request) {
        // 날짜는 필수
        if (request.getDate() == null || request.getDate().trim().isEmpty()) {
            throw new IllegalArgumentException("날짜는 필수입니다.");
        }

        List<Long> followingIds = userFollowRepo.findFollowingIds(request.getUserId());

        List<Record> records;

        if ("latest".equals(request.getSortBy())) {
            // 최신순 - JPQL 사용
            LocalDate dateFilter = LocalDate.parse(request.getDate());
            String teamFilter = parseTeam(request.getTeam());
            Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

            records = recordRepo.findAllFeedRecordsByLatest(
                    request.getUserId(), followingIds, dateFilter, teamFilter, pageable
            );
        } else {
            // 인기순 (기본값) - Native Query 사용
            String followingIdsStr = convertListToPostgresArray(followingIds);
            String teamFilter = parseTeam(request.getTeam());
            int offset = request.getPage() * request.getSize();

            records = recordRepo.findAllFeedRecordsByPopularity(
                    request.getUserId(),
                    followingIdsStr,
                    request.getDate(),
                    teamFilter,
                    request.getSize(),
                    offset
            );
        }

        return records.stream()
                .map(this::convertToFeedResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeedResponse> getFollowingFeed(FeedRequest request) {
        // 날짜는 필수
        if (request.getDate() == null || request.getDate().trim().isEmpty()) {
            throw new IllegalArgumentException("날짜는 필수입니다.");
        }

        List<Long> followingIds = userFollowRepo.findFollowingIds(request.getUserId());

        if (followingIds.isEmpty()) {
            return List.of();
        }

        List<Record> records;

        if ("latest".equals(request.getSortBy())) {
            // 최신순 - JPQL 사용
            LocalDate dateFilter = LocalDate.parse(request.getDate());
            String teamFilter = parseTeam(request.getTeam());
            Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

            records = recordRepo.findFollowingFeedRecordsByLatest(
                    followingIds, dateFilter, teamFilter, pageable
            );
        } else {
            // 인기순 (기본값) - Native Query 사용
            String userIdsStr = convertListToPostgresArray(followingIds);
            String teamFilter = parseTeam(request.getTeam());
            int offset = request.getPage() * request.getSize();

            records = recordRepo.findFollowingFeedRecordsByPopularity(
                    userIdsStr,
                    request.getDate(),
                    teamFilter,
                    request.getSize(),
                    offset
            );
        }

        return records.stream()
                .map(this::convertToFeedResponse)
                .collect(Collectors.toList());
    }

    // PostgreSQL 배열 형식으로 변환
    private String convertListToPostgresArray(List<Long> list) {
        if (list == null || list.isEmpty()) {
            return "{}";
        }
        return "{" + list.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")) + "}";
    }

    private String parseTeam(String team) {
        return (team != null && !team.trim().isEmpty()) ? team.trim() : null;
    }

    private FeedResponse convertToFeedResponse(Record record) {
        User user = userRepo.findById(record.getUserId()).orElseThrow();
        Game game = gameRepo.findById(record.getGameId()).orElseThrow();

        // 상위 3개 공감 스티커 조회
        TopReactionsResponse topReactions = reactionService.getTopReactions(record.getRecordId());
        Integer totalCount = reactionService.getTotalCount(record.getRecordId());

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
                .top3Reactions(topReactions.getTop3Reactions())
                .remainingReactionCount(topReactions.getRemainingCount())
                .totalReactionCount(totalCount)
                .build();
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
