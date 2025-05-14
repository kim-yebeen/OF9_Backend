package com.nine.baseballdiary.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    // 팔로잉 수 계산
    @Query("SELECT COUNT(uf) FROM UserFollow uf WHERE uf.follower.id = :userId")
    long countByFollowerId(Long userId);

    // 팔로워 수 계산
    @Query("SELECT COUNT(uf) FROM UserFollow uf WHERE uf.followee.id = :userId")
    long countByFolloweeId(Long userId);

    // 팔로우 여부 확인
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    // 팔로우 삭제
    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);}