package com.nine.baseballdiary.backend.report.service;

import com.nine.baseballdiary.backend.badge.Badge;
import com.nine.baseballdiary.backend.badge.BadgeRepository;
import com.nine.baseballdiary.backend.badge.UserBadgeRepository;
import com.nine.baseballdiary.backend.record.GameRecord;
import com.nine.baseballdiary.backend.record.GameRecordRepository;
import com.nine.baseballdiary.backend.report.dto.*;
import com.nine.baseballdiary.backend.user.repository.UserFollowRepository;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final UserRepository userRepository;
    private final GameRecordRepository gameRecordRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserFollowRepository userFollowRepository;
    public EmotionSummaryDto getEmotionSummary(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<GameRecord> monthlyRecords = gameRecordRepository.findByUserIdAndGameDateBetween(userId, startDate, endDate);

        Map<Integer, Long> emotionCounts = monthlyRecords.stream()
                .collect(Collectors.groupingBy(GameRecord::getEmotionCode, Collectors.counting()));

        // Top 3 감정 DTO 생성
        List<EmotionSummaryDto.EmotionCount> top3 = emotionCounts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(3)
                .map(entry -> new EmotionSummaryDto.EmotionCount(
                        convertEmotionToNoun(entry.getKey()),
                        entry.getValue().intValue(), // ✅ Long -> int 타입 변환
                        entry.getKey()))
                .collect(Collectors.toList());

        // 전체 감정 기록 DTO 생성
        List<EmotionSummaryDto.EmotionRecord> all = emotionCounts.entrySet().stream()
                .map(entry -> new EmotionSummaryDto.EmotionRecord(
                        convertEmotionLabel(entry.getKey()),
                        entry.getValue().intValue(), // ✅ Long -> int 타입 변환
                        entry.getKey()))
                .collect(Collectors.toList());

        return new EmotionSummaryDto(top3, all);
    }

    @Cacheable(value = "winRateSummary", key = "#userId")
    public WinRateSummaryDto getWinRateSummary(Long userId) {
        log.info("Calculating win rate for user {}", userId);
        List<GameRecord> records = gameRecordRepository.findByUserId(userId);

        long winCount = records.stream().filter(r -> "WIN".equals(r.getResult())).count();
        long loseCount = records.stream().filter(r -> "LOSE".equals(r.getResult())).count();
        long drawCount = records.stream().filter(r -> "DRAW".equals(r.getResult())).count();
        long totalGames = winCount + loseCount;
        double winRate = (totalGames == 0) ? 0.0 : ((double) winCount / totalGames) * 100.0;

        return WinRateSummaryDto.builder()
                .totalWinRate(winRate)
                .totalWinCount((int)winCount)
                .totalLoseCount((int)loseCount)
                .totalDrawCount((int)drawCount)
                .teamWinRates(List.of())
                .build();
    }

    public MyRankingDto getMyRanking(Long userId) {
        return MyRankingDto.builder().rank(999).build();
    }

    public List<UserRankingDto> getUserRankings() {
        return List.of();
    }

    public BadgeResponseDto getBadgeStatus(Long userId) {
        List<Badge> allBadges = badgeRepository.findAllByOrderByCategory();
        Set<Integer> myBadgeIds = userBadgeRepository.findAchievedBadgeIdsByUserId(userId);

        Map<String, List<BadgeResponseDto.BadgeDto>> groupedByCategory = allBadges.stream()
                .map(badge -> {
                    boolean isAchieved = myBadgeIds.contains(badge.getId());
                    return BadgeResponseDto.BadgeDto.builder()
                            .name(badge.getName())
                            .description(badge.getDescription())
                            .imageUrl(badge.getImageUrl())
                            .isAchieved(isAchieved)
                            .build();
                })
                .collect(Collectors.groupingBy(dto -> findCategoryNameByBadgeName(dto.getName())));

        List<BadgeResponseDto.BadgeCategoryDto> categories = groupedByCategory.entrySet().stream()
                .map(entry -> BadgeResponseDto.BadgeCategoryDto.builder()
                        .name(entry.getKey())
                        .badges(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        return BadgeResponseDto.builder()
                .totalBadgeCount(allBadges.size())
                .myBadgeCount(myBadgeIds.size())
                .categories(categories)
                .build();
    }

    private String convertEmotionToNoun(int code) {
        return switch(code) {
            case 1 -> "짜릿"; case 2 -> "만족"; case 3 -> "감동";
            case 4 -> "놀람"; case 5 -> "행복"; case 6 -> "답답";
            case 7 -> "아쉬움"; case 8 -> "화남"; case 9 -> "지침";
            default -> "기타";
        };
    }

    private String convertEmotionLabel(int code) {
        return switch(code) {
            case 1 -> "짜릿해요"; case 2 -> "만족해요"; case 3 -> "감동이에요";
            case 4 -> "놀랐어요"; case 5 -> "행복해요"; case 6 -> "답답해요";
            case 7 -> "아쉬워요"; case 8 -> "화났어요"; case 9 -> "지쳤어요";
            default -> "기타";
        };
    }

    private String findCategoryNameByBadgeName(String badgeName) {
        if (badgeName.contains("정복")) return "구단 도장깨기";
        if (badgeName.contains("직관")) return "직관 기록 수";
        if (badgeName.contains("승리")) return "승리요정";
        if (badgeName.contains("패배")) return "패배요정";
        return "감정 수집";
    }
}