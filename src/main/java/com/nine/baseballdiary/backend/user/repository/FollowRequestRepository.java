package com.nine.baseballdiary.backend.user.repository;

import com.nine.baseballdiary.backend.user.entity.FollowRequest;
import com.nine.baseballdiary.backend.user.entity.FollowRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FollowRequestRepository extends JpaRepository<FollowRequest, Long> {
    // 특정 대상에게 온 PENDING 요청 전체
    List<FollowRequest> findByTarget_IdAndStatus(Long targetId, FollowRequestStatus status);

    // requester/target 조합으로 단일 요청
    Optional<FollowRequest> findByRequester_IdAndTarget_Id(Long requesterId, Long targetId);
}