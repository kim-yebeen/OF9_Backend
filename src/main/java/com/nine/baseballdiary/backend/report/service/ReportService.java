package com.nine.baseballdiary.backend.report.service;

import com.nine.baseballdiary.backend.badge.Badge;
import com.nine.baseballdiary.backend.badge.BadgeRepository;
import com.nine.baseballdiary.backend.badge.UserBadge;
import com.nine.baseballdiary.backend.badge.UserBadgeRepository;
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

    public MainReportResponseDto getMainReport(Long userId) {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();

        return MainReportResponseDto.builder()
                .seasonInfo(getSeasonDday())
                .winRateInfo(getWinRateSummary(userId))
                .topEmotion(getTopEmotion(userId))
                .bestCompanion(getBestCompanion(userId))
                .badgeSummary(getBadgeSummary(userId))
                .topStadium(getTopStadium(userId))
                .bestAttendanceMonth(getBestAttendanceMonth(userId))
                .build();
    }

    //뱃지 요약 조회 (메인 페이지용)
    public BadgeSummaryDto getBadgeSummary(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        List<Badge> allBadges = badgeRepository.findAllByOrderByCategory();
        Set<Integer> myBadgeIds = userBadgeRepository.findAchievedBadgeIdsByUserId(userId);

        // 메인 페이지 5개 슬롯에 해당하는 뱃지명 결정
        String favTeam = user != null ? user.getFavTeam() : null;
        List<String> mainPageBadgeNames = getMainPageBadgeNames(favTeam);

        // 획득한 뱃지만 필터링
        List<UserBadge> allUserBadges = userBadgeRepository.findAllByUserId(userId);
        Map<String, UserBadge> nameToUserBadgeMap = allUserBadges.stream()
                .collect(Collectors.toMap(ub -> ub.getBadge().getName(), ub -> ub));

        // 메인 페이지 5개 슬롯 중 획득한 것만 반환
        List<BadgeSummaryDto.MainPageBadgeDto> mainPageBadges = new ArrayList<>();
        for (int i = 0; i < mainPageBadgeNames.size(); i++) {
            String badgeName = mainPageBadgeNames.get(i);
            UserBadge userBadge = nameToUserBadgeMap.get(badgeName);

            if (userBadge != null) {
                mainPageBadges.add(BadgeSummaryDto.MainPageBadgeDto.builder()
                        .badgeId(userBadge.getBadge().getId())
                        .badgeName(userBadge.getBadge().getName())
                        .imageUrl(userBadge.getBadge().getImageUrl())
                        .slotOrder(i + 1)  // 1~5
                        .build());
            }
        }

        return BadgeSummaryDto.builder()
                .totalBadgeCount(allBadges.size())
                .myBadgeCount(myBadgeIds.size())
                .mainPageBadges(mainPageBadges)
                .build();
    }

    /**
     * 메인 페이지 5개 슬롯의 뱃지명 결정 (응원팀에 따라 동적)
     */
    private List<String> getMainPageBadgeNames(String favTeam) {
        String teamConquestBadge = getTeamConquestBadgeNameByFavTeam(favTeam);

        return List.of(
                "기록의 시작",          // 슬롯 1
                teamConquestBadge,      // 슬롯 2 (동적)
                "응원의 보답",          // 슬롯 3
                "홈의 따뜻함",          // 슬롯 4
                "토닥토닥"             // 슬롯 5
        );
    }

    /**
     * 응원팀에 따른 팀 정복 뱃지명 반환
     */
    private String getTeamConquestBadgeNameByFavTeam(String favTeam) {
        if (favTeam == null) return "베어스 정복"; // 기본값

        return switch(favTeam) {
            case "두산 베어스" -> "베어스 정복";
            case "롯데 자이언츠" -> "갈매기 정복";
            case "삼성 라이온즈" -> "사자 정복";
            case "키움 히어로즈" -> "히어로 정복";
            case "한화 이글스" -> "독수리 정복";
            case "KIA 타이거즈" -> "호랑이 정복";
            case "KT WIZ" -> "마법사 정복";
            case "LG 트윈스" -> "쌍둥이 정복";
            case "NC 다이노스" -> "공룡 정복";
            case "SSG 랜더스" -> "랜더스 정복";
            default -> "베어스 정복";
        };
    }

    @Cacheable(value = "winRateSummary", key = "#userId")
    public WinRateSummaryDto getWinRateSummary(Long userId) {
        log.info("Calculating win rate for user {}", userId);
        List<GameRecord> records = gameRecordRepository.findByUserId(userId);
        User user = userRepository.findById(userId).orElse(null);
        String favTeam = user != null ? user.getFavTeam() : null;
        String shortFavTeam = convertFavTeam(favTeam);

        long totalWins = records.stream().filter(r -> "WIN".equals(r.getResult())).count();
        long totalLosses = records.stream().filter(r -> "LOSE".equals(r.getResult())).count();
        long totalDraws = records.stream().filter(r -> "DRAW".equals(r.getResult())).count();
        int totalGames = records.size();
        double totalWinRate = (totalWins + totalLosses == 0) ? 0.0 : Math.round(((double) totalWins / (totalWins + totalLosses)) * 1000.0) / 10.0;

        List<GameRecord> homeGames = records.stream()
                .filter(r -> r.getGame() != null && shortFavTeam != null && shortFavTeam.equals(r.getGame().getHomeTeam()))
                .collect(Collectors.toList());

        long homeWins = homeGames.stream().filter(r -> "WIN".equals(r.getResult())).count();
        long homeLosses = homeGames.stream().filter(r -> "LOSE".equals(r.getResult())).count();
        int homeGameCount = homeGames.size();
        double homeWinRate = (homeWins + homeLosses == 0) ? 0.0 : Math.round(((double) homeWins / (homeWins + homeLosses)) * 1000.0) / 10.0;

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

        // 획득한 뱃지만 조회
        List<UserBadge> allUserBadges = userBadgeRepository.findAllByUserId(userId);
        Map<Integer, UserBadge> badgeIdToUserBadgeMap = allUserBadges.stream()
                .collect(Collectors.toMap(ub -> ub.getBadge().getId(), ub -> ub));

        // ✅ 획득한 뱃지만 필터링
        Map<String, List<BadgeResponseDto.BadgeDto>> groupedByCategory = allBadges.stream()
                .filter(badge -> myBadgeIds.contains(badge.getId()))  // 획득한 것만
                .map(badge -> {
                    UserBadge userBadge = badgeIdToUserBadgeMap.get(badge.getId());

                    return BadgeResponseDto.BadgeDto.builder()
                            .name(badge.getName())
                            .description(badge.getDescription())
                            .imageUrl(badge.getImageUrl())
                            .isAchieved(true)  // 모두 true
                            .achievedAt(userBadge != null ? userBadge.getAchievedAt() : null)
                            .build();
                })
                .collect(Collectors.groupingBy(dto -> findCategoryNameByBadgeName(dto.getName())));

        // ✅ 획득한 뱃지가 있는 카테고리만 포함
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

        LocalDate regularSeasonStart = LocalDate.of(currentYear, 3, 23);
        LocalDate regularSeasonEnd = LocalDate.of(currentYear, 10, 15);
        LocalDate postSeasonStart = LocalDate.of(currentYear, 10, 25);
        LocalDate postSeasonEnd = LocalDate.of(currentYear, 11, 2);
        LocalDate nextRegularSeasonStart = LocalDate.of(currentYear + 1, 3, 28);

        if (today.isBefore(regularSeasonStart)) {
            int daysUntilStart = (int) ChronoUnit.DAYS.between(today, regularSeasonStart);
            return SeasonDdayDto.builder()
                    .seasonYear(currentYear)
                    .daysRemaining(daysUntilStart)
                    .seasonEndDate(regularSeasonStart.toString())
                    .status("BEFORE_START")
                    .message(currentYear + " 정규시즌 시작까지")
                    .build();
        } else if (today.isBefore(regularSeasonEnd) || today.isEqual(regularSeasonEnd)) {
            int daysUntilEnd = (int) ChronoUnit.DAYS.between(today, regularSeasonEnd);
            return SeasonDdayDto.builder()
                    .seasonYear(currentYear)
                    .daysRemaining(daysUntilEnd)
                    .seasonEndDate(regularSeasonEnd.toString())
                    .status("IN_PROGRESS_REGULAR")
                    .message(currentYear + " 정규 시즌 종료까지")
                    .build();
        } else if (today.isBefore(postSeasonStart)) {
            int daysUntilPSStart = (int) ChronoUnit.DAYS.between(today, postSeasonStart);
            return SeasonDdayDto.builder()
                    .seasonYear(currentYear)
                    .daysRemaining(daysUntilPSStart)
                    .seasonEndDate(postSeasonStart.toString())
                    .status("BEFORE_POSTSEASON")
                    .message(currentYear + " 포스트 시즌 시작까지")
                    .build();
        } else if (today.isBefore(postSeasonEnd) || today.isEqual(postSeasonEnd)) {
            int daysUntilPSEnd = (int) ChronoUnit.DAYS.between(today, postSeasonEnd);
            return SeasonDdayDto.builder()
                    .seasonYear(currentYear)
                    .daysRemaining(daysUntilPSEnd)
                    .seasonEndDate(postSeasonEnd.toString())
                    .status("IN_PROGRESS_POSTSEASON")
                    .message(currentYear + " 포스트 시즌 종료까지")
                    .build();
        } else {
            int daysUntilNextStart = (int) ChronoUnit.DAYS.between(today, nextRegularSeasonStart);
            return SeasonDdayDto.builder()
                    .seasonYear(currentYear + 1)
                    .daysRemaining(daysUntilNextStart)
                    .seasonEndDate(nextRegularSeasonStart.toString())
                    .status("ENDED")
                    .message((currentYear + 1) + " 정규시즌 시작까지")
                    .build();
        }
    }

    public CompanionStatsDto getBestCompanion(Long userId) {
        List<GameRecord> records = gameRecordRepository.findByUserId(userId);

        Map<Long, Long> companionCounts = records.stream()
                .filter(r -> r.getCompanions() != null && !r.getCompanions().isEmpty())
                .flatMap(r -> r.getCompanions().stream())
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        if (companionCounts.isEmpty()) {
            return null;
        }

        Map.Entry<Long, Long> topEntry = companionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (topEntry == null) {
            return null;
        }

        Long companionId = topEntry.getKey();
        int count = topEntry.getValue().intValue();

        User companion = userRepository.findById(companionId).orElse(null);
        if (companion == null) return null;

        WinRateSummaryDto winRateData = getWinRateSummary(companionId);

        return CompanionStatsDto.builder()
                .userId(companion.getId())
                .nickname(companion.getNickname())
                .profileImageUrl(companion.getProfileImageUrl())
                .companionCount(count)
                .winRate(winRateData.getTotalWinRate())
                .build();
    }

    public TopEmotionDto getTopEmotion(Long userId) {
        List<GameRecord> allRecords = gameRecordRepository.findByUserId(userId);

        Map<Integer, Long> emotionCounts = allRecords.stream()
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
                .emotion(convertEmotionLabel(topEntry.getKey()))
                .count(topEntry.getValue().intValue())
                .emotionCode(topEntry.getKey())
                .build();
    }

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

    public BestMonthDto getBestAttendanceMonth(Long userId) {
        List<GameRecord> allRecords = gameRecordRepository.findByUserId(userId);

        if (allRecords.isEmpty()) {
            return null;
        }

        Map<String, Long> yearMonthCounts = allRecords.stream()
                .filter(r -> r.getGame() != null && r.getGame().getDate() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getGame().getDate().getYear() + "-" + r.getGame().getDate().getMonthValue(),
                        Collectors.counting()
                ));

        if (yearMonthCounts.isEmpty()) {
            return null;
        }

        Map.Entry<String, Long> topEntry = yearMonthCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        String[] parts = topEntry.getKey().split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        return BestMonthDto.builder()
                .year(year)
                .month(month)
                .count(topEntry.getValue().intValue())
                .rate(null)
                .build();
    }


    private String convertEmotionLabel(int code) {
        return switch(code) {
            case 1 -> "행복해요";
            case 2 -> "놀랐어요";
            case 3 -> "짜릿해요";
            case 4 -> "벅차요";
            case 5 -> "통쾌해요";
            case 6 -> "만족해요";
            case 7 -> "지루해요";
            case 8 -> "무난해요";
            case 9 -> "긴장돼요";
            case 10 -> "질투나요";
            case 11 -> "답답해요";
            case 12 -> "아쉬워요";
            case 13 -> "지쳤어요";
            case 14 -> "허탈해요";
            case 15 -> "짜증나요";
            case 16 -> "화나요";
            default -> "기타";
        };
    }

    private String findCategoryNameByBadgeName(String badgeName) {
        if (badgeName.equals("기록의 시작") || badgeName.equals("홈의 따뜻함") ||
                badgeName.equals("원정의 즐거움") || badgeName.equals("같이 응원해요") ||
                badgeName.equals("속닥속닥")) {
            return "어서와, 야구 직관은 처음이지?";
        }
        if (badgeName.contains("응원의 보답") || badgeName.contains("네잎클로버") ||
                badgeName.contains("행운의 편지")) {
            return "나는야 승리요정";
        }
        if (badgeName.contains("토닥토닥") || badgeName.contains("그래도 응원해") ||
                badgeName.contains("이게 사랑이야")) {
            return "패배해도 괜찮아";
        }
        if (badgeName.contains("정복")) {
            return "모든 야구장을 제패하겠어";
        }
        return "기타";
    }

    private String getCategoryDisplayName(String category) {
        return switch(category) {
            case "FIRST_TIMER" -> "어서와, 야구 직관은 처음이지?";
            case "WINS" -> "나는야 승리요정";
            case "LOSSES" -> "패배해도 괜찮아";
            case "TEAM_CONQUEST" -> "모든 야구장을 제패하겠어";
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
        if (fav == null) return null;
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