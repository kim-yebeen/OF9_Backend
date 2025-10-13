package com.nine.baseballdiary.backend.user.repository;

import com.nine.baseballdiary.backend.user.entity.FollowRequest;
import com.nine.baseballdiary.backend.user.entity.FollowRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FollowRequestRepository extends JpaRepository<FollowRequest, Long> {
    // 특정 대상에게 온 PENDING 요청 전체
    List<FollowRequest> findByTarget_IdAndStatus(Long targetId, FollowRequestStatus status);

    // requester/target 조합으로 단일 요청
    Optional<FollowRequest> findByRequester_IdAndTarget_Id(Long requesterId, Long targetId);

    // 검색 기능을 위한 메서드 추가 (팔로우 상태 확인용)
    boolean existsByRequester_IdAndTarget_IdAndStatus(Long requesterId, Long targetId, FollowRequestStatus status);

    // 팔로우 요청 삭제 (차단 시 사용)
    void deleteByRequester_IdAndTarget_Id(Long requesterId, Long targetId);

    @Modifying
    @Query("DELETE FROM FollowRequest fr WHERE (fr.requester.id = :userId1 AND fr.target.id = :userId2) OR (fr.requester.id = :userId2 AND fr.target.id = :userId1)")
    void deleteByBothUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Query("SELECT fr.target.id FROM FollowRequest fr WHERE fr.requester.id = :currentUserId AND fr.target.id IN :targetUserIds AND fr.status = 'PENDING'")
    Set<Long> findPendingRequestTargetIdsByRequesterIdAndInTargetUserIds(@Param("currentUserId") Long currentUserId, @Param("targetUserIds") List<Long> targetUserIds);

    @Modifying
    @Query("DELETE FROM FollowRequest fr WHERE fr.requester.id = :userId OR fr.target.id = :userId")
    void deleteAllByRequesterIdOrTargetId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM FollowRequest fr WHERE fr.requester.id = :requesterId AND fr.target.id = :targetId AND fr.status = :status")
    int deleteByRequester_IdAndTarget_IdAndStatus(@Param("requesterId") Long requesterId,
                                                  @Param("targetId") Long targetId);
}