package com.nine.baseballdiary.backend.badge;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    // 사용자가 획득한 뱃지 ID 목록을 조회 (Integer 타입으로 원복)
    @Query("SELECT ub.badge.id FROM UserBadge ub WHERE ub.user.id = :userId")
    Set<Integer> findAchievedBadgeIdsByUserId(@Param("userId") Long userId);

    // 최근 획득한 뱃지 조회 메서드 추가
    @Query("SELECT ub FROM UserBadge ub JOIN FETCH ub.badge WHERE ub.user.id = :userId ORDER BY ub.achievedAt DESC")
    List<UserBadge> findTop5ByUserIdOrderByAchievedAtDesc(@Param("userId") Long userId, Pageable pageable);

    // 편의 메서드
    default List<UserBadge> findTop5ByUserIdOrderByAchievedAtDesc(Long userId) {
        return findTop5ByUserIdOrderByAchievedAtDesc(userId, PageRequest.of(0, 5));
    }
}