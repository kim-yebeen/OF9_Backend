package com.nine.baseballdiary.backend.report.service;

import com.nine.baseballdiary.backend.badge.Badge;
import com.nine.baseballdiary.backend.badge.BadgeRepository;
import com.nine.baseballdiary.backend.badge.UserBadge;
import com.nine.baseballdiary.backend.badge.UserBadgeRepository;
import com.nine.baseballdiary.backend.game.Game;
import com.nine.baseballdiary.backend.record.GameRecord;
import com.nine.baseballdiary.backend.record.GameRecordRepository;
import com.nine.baseballdiary.backend.user.entity.User;
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
            case "ATTENDANCE_COUNT":
                return records.size() >= badge.getThreshold();

            case "FAV_TEAM_ATTENDANCE":
                long favTeamRecordCount = records.stream()
                        .filter(r -> {
                            Game game = r.getGame();
                            if (game == null) return false;
                            String favTeam = user.getFavTeam();
                            return favTeam.equals(game.getHomeTeam()) || favTeam.equals(game.getAwayTeam());
                        })
                        .count();
                return favTeamRecordCount >= badge.getThreshold();

            case "WINS":
                long winCount = records.stream().filter(r -> "WIN".equals(r.getResult())).count();
                return winCount >= badge.getThreshold();

            case "LOSSES":
                long loseCount = records.stream().filter(r -> "LOSE".equals(r.getResult())).count();
                return loseCount >= badge.getThreshold();

            case "STADIUM_CONQUEST":
                Set<String> visitedStadiums = records.stream().map(GameRecord::getStadium).collect(Collectors.toSet());
                return checkStadiumBadge(visitedStadiums, badge.getName());

            case "EMOTION_COLLECTION":
                if ("감정수집가".equals(badge.getName())) {
                    long distinctEmotions = records.stream().map(GameRecord::getEmotionCode).distinct().count();
                    return distinctEmotions >= badge.getThreshold();
                } else {
                    long emotionCount = records.stream().filter(r -> isMatchingEmotion(r.getEmotionCode(), badge.getName())).count();
                    return emotionCount >= badge.getThreshold();
                }
            default:
                return false;
        }
    }

    private boolean checkStadiumBadge(Set<String> visitedStadiums, String badgeName) {
        return switch (badgeName) {
            case "잠실 정복" -> visitedStadiums.contains("잠실");
            case "고척 정복" -> visitedStadiums.contains("고척");
            case "부산 정복" -> visitedStadiums.contains("사직");
            case "대구 정복" -> visitedStadiums.contains("대구");
            case "광주 정복" -> visitedStadiums.contains("광주");
            case "대전 정복" -> visitedStadiums.contains("대전(신)");
            case "창원 정복" -> visitedStadiums.contains("창원");
            case "인천 정복" -> visitedStadiums.contains("문학");
            case "수원 정복" -> visitedStadiums.contains("수원");
            default -> false;
        };
    }

    private boolean isMatchingEmotion(int emotionCode, String badgeName) {
        return switch (badgeName) {
            case "짜릿함 중독" -> emotionCode == 1;
            case "만족의 미학" -> emotionCode == 2;
            case "감동주의보" -> emotionCode == 3;
            case "예측불가!" -> emotionCode == 4;
            case "행복전도사" -> emotionCode == 5;
            case "고구마 먹방" -> emotionCode == 6;
            case "조금만 더…" -> emotionCode == 7;
            case "분노의 질주" -> emotionCode == 8;
            case "피로회복제" -> emotionCode == 9;
            default -> false;
        };
    }
}