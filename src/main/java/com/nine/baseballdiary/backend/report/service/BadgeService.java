package com.nine.baseballdiary.backend.report.service;

import com.nine.baseballdiary.backend.badge.Badge;
import com.nine.baseballdiary.backend.badge.BadgeRepository;
import com.nine.baseballdiary.backend.badge.UserBadge;
import com.nine.baseballdiary.backend.badge.UserBadgeRepository;
import com.nine.baseballdiary.backend.comment.RecordCommentRepository;
import com.nine.baseballdiary.backend.game.Game;
import com.nine.baseballdiary.backend.record.GameRecord;
import com.nine.baseballdiary.backend.record.GameRecordRepository;
import com.nine.baseballdiary.backend.user.entity.User;
import com.nine.baseballdiary.backend.user.repository.UserFollowRepository;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BadgeService {

    private final UserRepository userRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final GameRecordRepository gameRecordRepository;
    private final UserFollowRepository userFollowRepository;
    private final RecordCommentRepository recordCommentRepository;

    @Async
    public void checkAndAwardBadgesForUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        List<Badge> allBadges = badgeRepository.findAll();
        Set<Integer> achievedBadgeIds = userBadgeRepository.findAchievedBadgeIdsByUserId(userId);
        List<GameRecord> userRecords = gameRecordRepository.findByUserId(userId);

        for (Badge badge : allBadges) {
            if (achievedBadgeIds.contains(badge.getId())) continue;

            if (checkBadgeAchievement(user, badge, userRecords)) {
                UserBadge userBadge = UserBadge.builder().user(user).badge(badge).build();
                userBadgeRepository.save(userBadge);
            }
        }
    }

    public void checkAndAwardBadgesForAllUsers() {
        List<User> allUsers = userRepository.findAll();
        List<Badge> allBadges = badgeRepository.findAll();

        for (User user : allUsers) {
            Set<Integer> achievedBadgeIds = userBadgeRepository.findAchievedBadgeIdsByUserId(user.getId());
            List<GameRecord> userRecords = gameRecordRepository.findByUserId(user.getId());

            for (Badge badge : allBadges) {
                if (achievedBadgeIds.contains(badge.getId())) {
                    continue;
                }

                boolean isAchieved = checkBadgeAchievement(user, badge, userRecords);

                if (isAchieved) {
                    UserBadge userBadge = UserBadge.builder().user(user).badge(badge).build();
                    userBadgeRepository.save(userBadge);
                }
            }
        }
    }

    private boolean checkBadgeAchievement(User user, Badge badge, List<GameRecord> records) {
        switch (badge.getCategory()) {
            case "FIRST_TIMER":
                return checkFirstTimerBadge(user, badge, records);

            case "WINS":
                long winCount = records.stream().filter(r -> "WIN".equals(r.getResult())).count();
                return winCount >= badge.getThreshold();

            case "LOSSES":
                long loseCount = records.stream().filter(r -> "LOSE".equals(r.getResult())).count();
                return loseCount >= badge.getThreshold();

            case "TEAM_CONQUEST":
                return checkTeamConquestBadge(badge, records);

            default:
                return false;
        }
    }

    /**
     * FIRST_TIMER 카테고리 뱃지 체크
     */
    private boolean checkFirstTimerBadge(User user, Badge badge, List<GameRecord> records) {
        switch (badge.getName()) {
            case "기록의 시작":
                // 직관 기록 1회
                return records.size() >= badge.getThreshold();

            case "홈의 따뜻함":
                // 응원팀이 홈팀인 경기 3회
                String favTeam = convertFavTeamToShort(user.getFavTeam());
                long homeCount = records.stream()
                        .filter(r -> {
                            Game game = r.getGame();
                            if (game == null || favTeam == null) return false;
                            return favTeam.equals(game.getHomeTeam());
                        })
                        .count();
                return homeCount >= badge.getThreshold();

            case "원정의 즐거움":
                // 응원팀이 원정팀인 경기 3회
                String favTeamAway = convertFavTeamToShort(user.getFavTeam());
                long awayCount = records.stream()
                        .filter(r -> {
                            Game game = r.getGame();
                            if (game == null || favTeamAway == null) return false;
                            return favTeamAway.equals(game.getAwayTeam());
                        })
                        .count();
                return awayCount >= badge.getThreshold();

            case "같이 응원해요":
                // 맞팔 친구 3명
                List<Long> following = userFollowRepository.findFollowingIds(user.getId());
                List<Long> followers = userFollowRepository.findFollowerIds(user.getId());
                long mutualFollowCount = following.stream()
                        .filter(followers::contains)
                        .count();
                return mutualFollowCount >= badge.getThreshold();

            case "속닥속닥":
                // 댓글 3회 작성
                long commentCount = recordCommentRepository.countByUserIdAndDeletedAtIsNull(user.getId());
                return commentCount >= badge.getThreshold();

            default:
                return false;
        }
    }

    /**
     * TEAM_CONQUEST 카테고리 뱃지 체크
     * 각 팀별 경기 5회 직관 (홈/원정 구분 없이)
     */
    private boolean checkTeamConquestBadge(Badge badge, List<GameRecord> records) {
        String targetTeam = getTeamFromBadgeName(badge.getName());
        if (targetTeam == null) return false;

        long teamGameCount = records.stream()
                .filter(r -> {
                    Game game = r.getGame();
                    if (game == null) return false;
                    return targetTeam.equals(game.getHomeTeam()) || targetTeam.equals(game.getAwayTeam());
                })
                .count();

        return teamGameCount >= badge.getThreshold();
    }

    /**
     * 뱃지 이름에서 팀 코드 추출
     */
    private String getTeamFromBadgeName(String badgeName) {
        return switch (badgeName) {
            case "베어스 정복" -> "두산";
            case "갈매기 정복" -> "롯데";
            case "사자 정복" -> "삼성";
            case "히어로 정복" -> "키움";
            case "독수리 정복" -> "한화";
            case "호랑이 정복" -> "KIA";
            case "마법사 정복" -> "KT";
            case "쌍둥이 정복" -> "LG";
            case "공룡 정복" -> "NC";
            case "랜더스 정복" -> "SSG";
            default -> null;
        };
    }

    /**
     * 응원팀 전체 이름을 짧은 코드로 변환
     */
    private String convertFavTeamToShort(String favTeam) {
        if (favTeam == null) return null;
        return switch (favTeam) {
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
            default -> favTeam;
        };
    }
}