package com.nine.baseballdiary.backend.feed;

import com.nine.baseballdiary.backend.record.Record;
import com.nine.baseballdiary.backend.record.RecordRepository;
import com.nine.baseballdiary.backend.game.Game;
import com.nine.baseballdiary.backend.game.GameRepository;
import com.nine.baseballdiary.backend.reaction.ReactionService;
import com.nine.baseballdiary.backend.reaction.RecordReactionSummary;

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
import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * 전체 피드 조회
     * - 모든 공개 계정의 게시물
     * - 내가 팔로우한 비공개 계정의 게시물
     * - 내 자신의 게시물 (비공개여도 표시)
     */
    @Transactional(readOnly = true)
    public List<FeedResponse> getAllFeed(FeedRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        // 날짜 필터 파싱
        LocalDate dateFilter = parseDate(request.getDate());

        // 팀 필터 처리
        String teamFilter = parseTeam(request.getTeam());

        // 내가 팔로우한 사람들의 ID 조회
        List<Long> followingIds = userFollowRepo.findFollowingIds(request.getUserId());

        System.out.println("=== 전체 피드 디버깅 ===");
        System.out.println("현재 사용자 ID: " + request.getUserId());
        System.out.println("팔로우한 사용자 수: " + followingIds.size());
        System.out.println("팔로우한 사용자 IDs: " + followingIds);

        List<Record> records = recordRepo.findAllFeedRecords(
                request.getUserId(),
                followingIds,
                dateFilter,
                teamFilter,
                pageable
        );

        System.out.println("조회된 레코드 수: " + records.size());

        return records.stream()
                .map(this::convertToFeedResponse)
                .collect(Collectors.toList());
    }

    /**
     * 팔로잉 피드 조회
     * - 내가 팔로우한 사람들의 게시물만 (공개/비공개 상관없이)
     * - 내 게시물은 포함하지 않음
     */
    @Transactional(readOnly = true)
    public List<FeedResponse> getFollowingFeed(FeedRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        // 날짜 필터 파싱
        LocalDate dateFilter = parseDate(request.getDate());

        // 팀 필터 처리
        String teamFilter = parseTeam(request.getTeam());

        // 내가 팔로우한 사람들만 (나 자신 제외)
        List<Long> followingIds = userFollowRepo.findFollowingIds(request.getUserId());

        System.out.println("=== 팔로잉 피드 디버깅 ===");
        System.out.println("현재 사용자 ID: " + request.getUserId());
        System.out.println("팔로우한 사용자 수: " + followingIds.size());
        System.out.println("팔로우한 사용자 IDs: " + followingIds);

        // 팔로우한 사람이 없으면 빈 리스트 반환
        if (followingIds.isEmpty()) {
            System.out.println("팔로우한 사용자가 없음");
            return List.of();
        }

        List<Record> records = recordRepo.findFollowingFeedRecords(
                followingIds,  // 나 자신 제외, 팔로우한 사람들만
                dateFilter,
                teamFilter,
                pageable
        );

        System.out.println("조회된 레코드 수: " + records.size());

        return records.stream()
                .map(this::convertToFeedResponse)
                .collect(Collectors.toList());
    }

    // 안전한 날짜 파싱
    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString.trim());
        } catch (Exception e) {
            System.out.println("날짜 파싱 오류: " + dateString);
            return null;
        }
    }

    // 안전한 팀 필터 처리
    private String parseTeam(String team) {
        return (team != null && !team.trim().isEmpty()) ? team.trim() : null;
    }

    private FeedResponse convertToFeedResponse(Record record) {
        User user = userRepo.findById(record.getUserId()).orElseThrow();
        Game game = gameRepo.findById(record.getGameId()).orElseThrow();

        RecordReactionSummary summary = reactionService.getSummary(record.getRecordId());

        // 안전한 mediaUrls 처리
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
                .reactions(summary.getStats())
                .totalReactionCount(summary.getTotalCount())
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
