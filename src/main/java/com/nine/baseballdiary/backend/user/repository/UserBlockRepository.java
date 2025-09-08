package com.nine.baseballdiary.backend.user.repository;

import com.nine.baseballdiary.backend.user.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    // 내가 차단한 사용자의 ID 목록 조회 (새로 추가)
    @Query("SELECT ub.blocked.id FROM UserBlock ub WHERE ub.blocker.id = :userId")
    Set<Long> findBlockedIdsByBlockerId(@Param("userId") Long userId);

    // 나를 차단한 사용자의 ID 목록 조회 (새로 추가)
    @Query("SELECT ub.blocker.id FROM UserBlock ub WHERE ub.blocked.id = :userId")
    Set<Long> findBlockerIdsByBlockedId(@Param("userId") Long userId);

}