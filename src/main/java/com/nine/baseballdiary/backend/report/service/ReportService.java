package com.nine.baseballdiary.backend.report.service;

import com.nine.baseballdiary.backend.badge.Badge;
import com.nine.baseballdiary.backend.badge.BadgeRepository;
import com.nine.baseballdiary.backend.badge.UserBadge;
import com.nine.baseballdiary.backend.badge.UserBadgeRepository;
import com.nine.baseballdiary.backend.player.PlayerInfoDto;
import com.nine.baseballdiary.backend.player.PlayerService;
import com.nine.baseballdiary.backend.record.GameRecord;
import com.nine.baseballdiary.backend.record.GameRecordRepository;
import com.nine.baseballdiary.backend.report.dto.*;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
    private final PlayerService playerService;

    // 전체 구장 목록
    private static final List<String> ALL_STADIUMS = Arrays.asList(
            "잠실", "고척", "사직", "대구", "광주", "대전(신)", "창원", "문학", "수원"
    );

    public MainReportResponseDto getMainReport(Long userId) {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();

        return MainReportResponseDto.builder()
                .seasonInfo(getSeasonDday())
                .winRateInfo(getWinRateSummary(userId))
                .topEmotion(getTopEmotion(userId, currentYear))          // 새 메서드
                .mvpPlayer(getMvpPlayer(userId))                         // 새 메서드 (단수형)
                .companionStats(getCompanionStats(userId))
                .badgeSummary(getBadgeSummary(userId))
                .topStadium(getTopStadium(userId))                       // 새 메서드
                .bestAttendanceMonth(getBestAttendanceMonth(userId, currentYear))  // 새 메서드
                .bestWinRateMonth(getBestWinRateMonth(userId, currentYear))        // 새 메서드
                .build();
    }


    //뱃지 요약 조회
    public BadgeSummaryDto getBadgeSummary(Long userId) {
        List<Badge> allBadges = badgeRepository.findAllByOrderByCategory();
        Set<Integer> myBadgeIds = userBadgeRepository.findAchievedBadgeIdsByUserId(userId);

        // 최근 획득한 뱃지 3개 조회
        List<UserBadge> recentUserBadges = userBadgeRepository.findTop3ByUserIdOrderByAchievedAtDesc(userId);
        List<BadgeSummaryDto.RecentBadgeDto> recentBadges = recentUserBadges.stream()
                .map(ub -> BadgeSummaryDto.RecentBadgeDto.builder()
                        .name(ub.getBadge().getName())
                        .imageUrl(ub.getBadge().getImageUrl())
                        .category(getCategoryDisplayName(ub.getBadge().getCategory()))
                        .build())
                .collect(Collectors.toList());

        return BadgeSummaryDto.builder()
                .totalBadgeCount(allBadges.size())
                .myBadgeCount(myBadgeIds.size())
                .recentBadges(recentBadges)
                .build();
    }



    /**
     * 승률 요약 조회 (홈/원정 포함)
     */
    @Cacheable(value = "winRateSummary", key = "#userId")
    public WinRateSummaryDto getWinRateSummary(Long userId) {
        log.info("Calculating win rate for user {}", userId);
        List<GameRecord> records = gameRecordRepository.findByUserId(userId);
        User user = userRepository.findById(userId).orElse(null);
        String favTeam = user != null ? user.getFavTeam() : null;
        String shortFavTeam = convertFavTeam(favTeam);

        // 전체 통계
        long totalWins = records.stream().filter(r -> "WIN".equals(r.getResult())).count();
        long totalLosses = records.stream().filter(r -> "LOSE".equals(r.getResult())).count();
        long totalDraws = records.stream().filter(r -> "DRAW".equals(r.getResult())).count();
        int totalGames = records.size();
        // getWinRateSummary 메서드의 모든 승률 계산에서
        double totalWinRate = (totalWins + totalLosses == 0) ? 0.0 : Math.round(((double) totalWins / (totalWins + totalLosses)) * 1000.0) / 10.0;

        // 홈 경기 통계 (응원팀이 홈팀인 경우)
        List<GameRecord> homeGames = records.stream()
                .filter(r -> r.getGame() != null && shortFavTeam != null && shortFavTeam.equals(r.getGame().getHomeTeam()))
                .collect(Collectors.toList());

        long homeWins = homeGames.stream().filter(r -> "WIN".equals(r.getResult())).count();
        long homeLosses = homeGames.stream().filter(r -> "LOSE".equals(r.getResult())).count();
        int homeGameCount = homeGames.size();
        double homeWinRate = (homeWins + homeLosses == 0) ? 0.0 : Math.round(((double) homeWins / (homeWins + homeLosses)) * 1000.0) / 10.0;

        // 원정 경기 통계 (응원팀이 원정팀인 경우)
        List<GameRecord> awayGames = records.stream()
                .filter(r -> r.getGame() != null && shortFavTeam != null && shortFavTeam.equals(r.getGame().getAwayTeam()))
                .collect(Collectors.toList());

        long awayWins = awayGames.stream().filter(r -> "WIN".equals(r.getResult())).count();
        long awayLosses = awayGames.stream().filter(r -> "LOSE".equals(r.getResult())).count();
        int awayGameCount = awayGames.size();
        double awayWinRate = (awayWins + awayLosses == 0) ? 0.0 : Math.round(((double) awayWins / (awayWins + awayLosses)) * 1000.0) / 10.0;

        return WinRateSummaryDto.builder()
                .totalWinRate(totalWinRate)
                .totalWinCount((int)totalWins)
                .totalLoseCount((int)totalLosses)
                .totalDrawCount((int)totalDraws)
                .totalGameCount(totalGames)
                .homeWinRate(homeWinRate)
                .homeWinCount((int)homeWins)
                .homeLoseCount((int)homeLosses)
                .homeGameCount(homeGameCount)
                .awayWinRate(awayWinRate)
                .awayWinCount((int)awayWins)
                .awayLoseCount((int)awayLosses)
                .awayGameCount(awayGameCount)
                .teamWinRates(List.of())
                .build();
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

    public SeasonDdayDto getSeasonDday() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();

        // 2025 시즌: 3월 23일 ~ 11월 15일
        LocalDate seasonStart = LocalDate.of(currentYear, 3, 23);
        LocalDate seasonEnd = LocalDate.of(currentYear, 11, 1);

        // 다음 시즌 시작일
        LocalDate nextSeasonStart = LocalDate.of(currentYear + 1, 3, 23);

        if (today.isBefore(seasonStart)) {
            // 시즌 시작 전
            int daysUntilStart = (int) ChronoUnit.DAYS.between(today, seasonStart);
            return SeasonDdayDto.builder()
                    .seasonYear(currentYear)
                    .daysRemaining(daysUntilStart)
                    .seasonEndDate(seasonStart.toString())
                    .status("BEFORE_START")
                    .message(currentYear + " 시즌 시작까지")
                    .build();
        } else if (today.isAfter(seasonEnd)) {
            // 시즌 종료 후
            int daysUntilNextStart = (int) ChronoUnit.DAYS.between(today, nextSeasonStart);
            return SeasonDdayDto.builder()
                    .seasonYear(currentYear + 1)
                    .daysRemaining(daysUntilNextStart)
                    .seasonEndDate(nextSeasonStart.toString())
                    .status("ENDED")
                    .message((currentYear + 1) + " 시즌 시작까지")
                    .build();
        } else {
            // 시즌 진행 중
            int daysUntilEnd = (int) ChronoUnit.DAYS.between(today, seasonEnd);
            return SeasonDdayDto.builder()
                    .seasonYear(currentYear)
                    .daysRemaining(daysUntilEnd)
                    .seasonEndDate(seasonEnd.toString())
                    .status("IN_PROGRESS")
                    .message(currentYear + " 시즌 종료까지")
                    .build();
        }
    }

    public List<CompanionStatsDto> getCompanionStats(Long userId) {
        List<GameRecord> records = gameRecordRepository.findByUserId(userId);

        Map<Long, Long> companionCounts = records.stream()
                .filter(r -> r.getCompanions() != null && !r.getCompanions().isEmpty())
                .flatMap(r -> r.getCompanions().stream())
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        return companionCounts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Long companionId = entry.getKey();
                    int count = entry.getValue().intValue();

                    User companion = userRepository.findById(companionId).orElse(null);
                    if (companion == null) return null;

                    // 캐시된 승률 조회
                    WinRateSummaryDto winRateData = getWinRateSummary(companionId);

                    return CompanionStatsDto.builder()
                            .userId(companion.getId())
                            .nickname(companion.getNickname())
                            .profileImageUrl(companion.getProfileImageUrl())
                            .companionCount(count)
                            .winRate(winRateData.getTotalWinRate())
                            .build();
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    /**
     * 이번 년도 가장 많이 선택한 감정 1개 조회
     */
    public TopEmotionDto getTopEmotion(Long userId, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<GameRecord> yearlyRecords = gameRecordRepository.findByUserIdAndGameDateBetween(userId, startDate, endDate);

        Map<Integer, Long> emotionCounts = yearlyRecords.stream()
                .collect(Collectors.groupingBy(GameRecord::getEmotionCode, Collectors.counting()));

        if (emotionCounts.isEmpty()) {
            return TopEmotionDto.builder()
                    .emotion("기타")
                    .count(0)
                    .emotionCode(0)
                    .build();
        }

        Map.Entry<Integer, Long> topEntry = emotionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        return TopEmotionDto.builder()
                .emotion(convertEmotionToNoun(topEntry.getKey()))
                .count(topEntry.getValue().intValue())
                .emotionCode(topEntry.getKey())
                .build();
    }

    /**
     * MVP 선수 1명 조회
     */
    public MvpPlayerDto getMvpPlayer(Long userId) {
        List<GameRecord> records = gameRecordRepository.findByUserId(userId);

        Map<String, Long> playerCounts = records.stream()
                .filter(r -> r.getBestPlayer() != null && !r.getBestPlayer().isBlank())
                .collect(Collectors.groupingBy(GameRecord::getBestPlayer, Collectors.counting()));

        if (playerCounts.isEmpty()) {
            return null;
        }

        Map.Entry<String, Long> topEntry = playerCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        String playerName = topEntry.getKey();
        int count = topEntry.getValue().intValue();

        String teamCode = findTeamByPlayerName(playerName);
        String teamName = convertTeamCodeToName(teamCode);

        return MvpPlayerDto.builder()
                .playerName(playerName)
                .team(teamName)
                .teamCode(teamCode)
                .count(count)
                .playerImageUrl(null)
                .build();
    }

    /**
     * 최다 방문 구장 1개 조회
     */
    public TopStadiumDto getTopStadium(Long userId) {
        List<GameRecord> records = gameRecordRepository.findByUserId(userId);

        Map<String, List<GameRecord>> stadiumRecords = records.stream()
                .filter(r -> r.getStadium() != null)
                .collect(Collectors.groupingBy(GameRecord::getStadium));

        if (stadiumRecords.isEmpty()) {
            return null;
        }

        Map.Entry<String, List<GameRecord>> topEntry = stadiumRecords.entrySet().stream()
                .max(Map.Entry.comparingByValue(Comparator.comparing(List::size)))
                .orElse(null);

        String stadium = topEntry.getKey();
        List<GameRecord> stadiumGames = topEntry.getValue();

        int visitCount = stadiumGames.size();
        long wins = stadiumGames.stream().filter(r -> "WIN".equals(r.getResult())).count();
        long losses = stadiumGames.stream().filter(r -> "LOSE".equals(r.getResult())).count();
        double winRate = (wins + losses == 0) ? 0.0 : Math.round(((double) wins / (wins + losses)) * 1000.0) / 10.0;

        return TopStadiumDto.builder()
                .stadiumName(stadium)
                .visitCount(visitCount)
                .winRate(winRate)
                .city(getStadiumCity(stadium))
                .build();
    }

    /**
     * 이번 년도 직관 많이 간 달 조회
     */
    public BestMonthDto getBestAttendanceMonth(Long userId, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<GameRecord> yearlyRecords = gameRecordRepository.findByUserIdAndGameDateBetween(userId, startDate, endDate);

        Map<Integer, Long> monthCounts = yearlyRecords.stream()
                .collect(Collectors.groupingBy(r -> r.getGame().getDate().getMonthValue(), Collectors.counting()));

        if (monthCounts.isEmpty()) {
            return null;
        }

        Map.Entry<Integer, Long> topEntry = monthCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        return BestMonthDto.builder()
                .year(year)
                .month(topEntry.getKey())
                .count(topEntry.getValue().intValue())
                .rate(null)
                .build();
    }

    public BestMonthDto getBestWinRateMonth(Long userId, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<GameRecord> yearlyRecords = gameRecordRepository.findByUserIdAndGameDateBetween(userId, startDate, endDate);

        Map<Integer, List<GameRecord>> monthRecords = yearlyRecords.stream()
                .collect(Collectors.groupingBy(r -> r.getGame().getDate().getMonthValue()));

        if (monthRecords.isEmpty()) {
            return null;
        }

        Map.Entry<Integer, Double> bestEntry = monthRecords.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= 2) // 최소 2경기
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<GameRecord> monthGames = entry.getValue();
                            long wins = monthGames.stream().filter(r -> "WIN".equals(r.getResult())).count();
                            long losses = monthGames.stream().filter(r -> "LOSE".equals(r.getResult())).count();
                            return (wins + losses == 0) ? 0.0 : ((double) wins / (wins + losses)) * 100.0;
                        }
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (bestEntry == null) {
            return null;
        }

        int bestMonth = bestEntry.getKey();
        double bestWinRate = Math.round(bestEntry.getValue() * 10.0) / 10.0; // 소숫점 1자리
        int gameCount = monthRecords.get(bestMonth).size();

        return BestMonthDto.builder()
                .year(year)
                .month(bestMonth)
                .count(gameCount)
                .rate(bestWinRate)
                .build();
    }

    // Private helper methods
    private String findTeamByPlayerName(String playerName) {
        List<PlayerInfoDto> result = playerService.searchPlayers(playerName);
        return result.isEmpty() ? "XX" : result.get(0).getTeam();
    }


    private String convertTeamCodeToName(String code) {
        return switch(code) {
            case "KIA" -> "KIA타이거즈";
            case "NC" -> "NC다이노스";
            case "삼성" -> "삼성라이온즈";
            case "LG" -> "LG트윈스";
            case "두산" -> "두산베어스";
            case "KT" -> "KT WIZ";
            case "SSG" -> "SSG랜더스";
            case "롯데" -> "롯데자이언츠";
            case "한화" -> "한화이글스";
            case "키움" -> "키움히어로즈";
            default -> code;
        };
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
        if (badgeName.contains("직관")) return "직관 기록수";
        if (badgeName.contains("승리")) return "승리요정";
        if (badgeName.contains("패배")) return "패배요정";
        return "감정 수집";
    }

    private String getCategoryDisplayName(String category) {
        return switch(category) {
            case "STADIUM_CONQUEST" -> "구단 도장깨기";
            case "ATTENDANCE_COUNT" -> "직관 기록수";
            case "WINS" -> "승리요정";
            case "LOSSES" -> "패배요정";
            case "EMOTION_COLLECTION" -> "감정 수집";
            default -> category;
        };
    }

    private String getStadiumCity(String stadium) {
        return switch(stadium) {
            case "잠실" -> "서울";
            case "고척" -> "서울";
            case "사직" -> "부산";
            case "대구" -> "대구";
            case "광주" -> "광주";
            case "대전(신)" -> "대전";
            case "창원" -> "창원";
            case "문학" -> "인천";
            case "수원" -> "수원";
            default -> "기타";
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
}