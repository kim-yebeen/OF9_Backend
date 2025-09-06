package com.nine.baseballdiary.backend.user.repository;

import com.nine.baseballdiary.backend.user.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    // 특정 사용자가 특정 사용자를 차단했는지 확인
    boolean existsByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    // 차단 관계 조회
    Optional<UserBlock> findByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    // 내가 차단한 사용자 목록
    @Query("SELECT ub FROM UserBlock ub JOIN FETCH ub.blocked WHERE ub.blocker.id = :blockerId ORDER BY ub.createdAt DESC")
    List<UserBlock> findByBlocker_IdOrderByCreatedAtDesc(@Param("blockerId") Long blockerId);

    // 나를 차단한 사용자 목록 (필요시)
    List<UserBlock> findByBlocked_Id(Long blockedId);

    // 차단 관계 삭제
    void deleteByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);
}