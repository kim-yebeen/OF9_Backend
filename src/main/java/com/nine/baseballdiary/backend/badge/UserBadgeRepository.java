package com.nine.baseballdiary.backend.badge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    // 사용자가 획득한 뱃지 ID 목록을 조회 (효율적인 확인을 위해 Set 사용)
    @Query("SELECT ub.badge.id FROM UserBadge ub WHERE ub.user.id = :userId")
    Set<Integer> findAchievedBadgeIdsByUserId(Long userId);
}