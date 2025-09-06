package com.nine.baseballdiary.backend.user.service;

import com.nine.baseballdiary.backend.Notifiation.NotificationService;
import com.nine.baseballdiary.backend.reaction.RecordReactionRepository;
import com.nine.baseballdiary.backend.record.RecordRepository;
import com.nine.baseballdiary.backend.user.dto.*;
import com.nine.baseballdiary.backend.user.entity.*;
import com.nine.baseballdiary.backend.user.repository.FollowRequestRepository;
import com.nine.baseballdiary.backend.user.repository.UserBlockRepository;
import com.nine.baseballdiary.backend.user.repository.UserFollowRepository;
import com.nine.baseballdiary.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepo;
    private final UserFollowRepository followRepo;
    private final RecordRepository recordRepo;
    private final FollowRequestRepository reqRepo;
    private final NotificationService notificationService;
    private final UserBlockRepository userBlockRepo;
    private final RecordReactionRepository recordReactionRepo; // 기존 필드


    public List<UserDto> searchUsers(Long currentUserId, String q) {
        List<User> users = userRepo.findByNicknameContainingIgnoreCase(q);

        return users.stream()
                .filter(user -> !isBlockedEachOther(currentUserId, user.getId())) // 상호 차단된 사용자 제외
                .map(u -> new UserDto(u.getId(), u.getNickname(), u.getProfileImageUrl(), u.getFavTeam()))
                .toList();
    }

    /**
     * 1) 팔로우 요청 (공개면 즉시, 비공개면 PENDING 생성)
     */
    // ✅ 팔로우 요청 (차단된 사용자는 팔로우 불가)
    @Transactional
    public FollowResponse requestFollow(Long meId, Long targetId) {
        // 차단 여부 확인
        if (isBlockedEachOther(meId, targetId)) {
            throw new IllegalArgumentException("차단된 사용자와는 팔로우할 수 없습니다");
        }

        User me = userRepo.findById(meId).orElseThrow();
        User target = userRepo.findById(targetId).orElseThrow();

        // 이미 팔로우 중이면 아무 동작 없이 false 반환
        if (followRepo.existsByFollowerId_IdAndFolloweeId_Id(meId, targetId)) {
            return new FollowResponse(false, false, null);
        }

        if (Boolean.TRUE.equals(target.getIsPrivate())) {
            // 비공개 계정: PENDING 요청 생성
            FollowRequest req = FollowRequest.builder()
                    .requester(me)
                    .target(target)
                    .status(FollowRequestStatus.PENDING)
                    .build();
            req = reqRepo.save(req);
            notificationService.createFollowRequestNotification(targetId, meId);
            return new FollowResponse(true, true, req.getId());
        } else {
            // 공개 계정: 즉시 팔로우
            followRepo.save(new UserFollow(null, me, target));
            notificationService.createFollowNotification(targetId, meId);
            return new FollowResponse(true, false, null);
        }
    }

    // 2) 내 계정으로 온 PENDING 요청 리스트 조회
    //    (import org.springframework.transaction.annotation.Transactional;)
    @Transactional(readOnly = true)
    public List<FollowRequestDto> listIncomingRequests(Long me) {
        return reqRepo.findByTarget_IdAndStatus(me, FollowRequestStatus.PENDING)
                .stream()
                .map(req -> new FollowRequestDto(
                        req.getId(),
                        req.getRequester().getId(),
                        req.getRequester().getNickname(),
                        req.getCreatedAt()
                ))
                .toList();
    }

    /**
     * 3) 비공개 계정 주인이 수락
     */
    @Transactional
    public void acceptFollowRequest(Long me, Long requestId) {
        FollowRequest req = reqRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 요청입니다."));

        // 본인이 받은 요청이 아니면 403
        if (!req.getTarget().getId().equals(me)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "내 계정으로 들어온 요청만 승인할 수 있습니다."
            );
        }

        // 이미 처리된 요청이면 400
        if (req.getStatus() != FollowRequestStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이미 처리된 요청입니다."
            );
        }

        followRepo.save(new UserFollow(null, req.getRequester(), req.getTarget()));
        req.setStatus(FollowRequestStatus.ACCEPTED);
    }

    /**
     * 4) 비공개 계정 주인이 거절
     */
    @Transactional
    public void rejectFollowRequest(Long me, Long requestId) {
        FollowRequest req = reqRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 요청입니다."));

        if (!req.getTarget().getId().equals(me)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "내 계정으로 들어온 요청만 거절할 수 있습니다."
            );
        }

        if (req.getStatus() != FollowRequestStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이미 처리된 요청입니다."
            );
        }

        req.setStatus(FollowRequestStatus.REJECTED);
    }

    /**
     * 언팔로우
     */
    @Transactional
    public void unfollow(Long meId, Long targetId) {
        followRepo.deleteByFollowerId_IdAndFolloweeId_Id(meId, targetId);
    }

    // ✅ 팔로잉 목록 (차단된 사용자 제외)
    @Transactional(readOnly = true)
    public List<UserDto> getFollowing(Long userId, Long currentUserId) {
        return followRepo.findByFollowerId_Id(userId).stream()
                .map(uf -> uf.getFolloweeId())
                .filter(user -> !isBlockedEachOther(currentUserId, user.getId())) // 차단된 사용자 제외
                .map(u -> new UserDto(u.getId(), u.getNickname(), u.getProfileImageUrl(), u.getFavTeam()))
                .collect(Collectors.toList());
    }

    /// ✅ 팔로워 목록 (차단된 사용자 제외)
    @Transactional(readOnly = true)
    public List<UserDto> getFollowers(Long userId, Long currentUserId) {
        return followRepo.findByFolloweeId_Id(userId).stream()
                .map(uf -> uf.getFollowerId())
                .filter(user -> !isBlockedEachOther(currentUserId, user.getId())) // 차단된 사용자 제외
                .map(u -> new UserDto(u.getId(), u.getNickname(), u.getProfileImageUrl(), u.getFavTeam()))
                .collect(Collectors.toList());
    }

    // 내 프로필 조회
    public UserProfileDto getMyProfile(Long userId) {
        User u = userRepo.findById(userId).orElseThrow();
        long followerCnt = followRepo.findByFolloweeId_Id(userId).size();
        long followingCnt = followRepo.findByFollowerId_Id(userId).size();
        long recordCnt = recordRepo.countByUserId(userId);
        return new UserProfileDto(
                u.getId(), u.getNickname(), u.getProfileImageUrl(),
                u.getFavTeam(), u.getIsPrivate(),
                followerCnt, followingCnt, recordCnt
        );
    }

    // 내 정보 수정
    @Transactional
    public void updateUser(Long userId, UpdateUserRequest req) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 닉네임 중복 확인 (닉네임이 변경된 경우만)
        if (!u.getNickname().equals(req.getNickname())
                && userRepo.existsByNickname(req.getNickname())) {
            throw new IllegalArgumentException("이미 사용중인 닉네임입니다.");
        }

        // ✅ 필수 값들 업데이트
        u.setNickname(req.getNickname());
        u.setFavTeam(req.getFavTeam()); // 필수값이므로 validation에서 이미 체크됨

        // ✅ nullable 값 안전하게 처리 (profileImageUrl만)
        u.setProfileImageUrl(req.getProfileImageUrl() != null && !req.getProfileImageUrl().trim().isEmpty()
                ? req.getProfileImageUrl().trim() : null);

        // ✅ Boolean 처리 (null인 경우 기존 값 유지)
        if (req.getIsPrivate() != null) {
            u.setIsPrivate(req.getIsPrivate());
        }

        // ✅ @PreUpdate로 updatedAt이 자동 설정됨
        // JPA dirty checking으로 자동 업데이트
    }

    // 로그아웃: 경우에 따라 토큰 무효화 로직 추가
    public void logout(Long userId) { /* no-op or invalidate JWT */ }

    @Transactional
    public void deleteUser(Long userId) {
        userRepo.deleteById(userId);
    }

    public boolean isNicknameAvailable(String nickname) {
        // 닉네임 유효성 검사
        if (nickname == null || nickname.trim().isEmpty()) {
            return false;
        }

        if (nickname.length() < 1 || nickname.length() > 15) {
            return false;
        }

        if (!nickname.matches("^[가-힣a-zA-Z0-9\\s_-]+$")) {
            return false;
        }

        // 중복 확인
        return !userRepo.existsByNickname(nickname);
    }

    @Transactional
    public void blockUser(Long blockerId, Long targetId) {
        // 자기 자신을 차단할 수 없음
        if (blockerId.equals(targetId)) {
            throw new IllegalArgumentException("자기 자신을 차단할 수 없습니다");
        }

        // 대상 사용자가 존재하는지 확인
        User target = userRepo.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));

        User blocker = userRepo.findById(blockerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));

        // 이미 차단 중인지 확인
        if (userBlockRepo.existsByBlocker_IdAndBlocked_Id(blockerId, targetId)) {
            throw new IllegalArgumentException("이미 차단된 사용자입니다");
        }

        // 차단 처리
        UserBlock userBlock = UserBlock.builder()
                .blocker(blocker)
                .blocked(target)
                .build();

        userBlockRepo.save(userBlock);

        // 차단 시 팔로우 관계 정리
        // 1. 내가 상대방을 팔로우하고 있다면 언팔로우
        followRepo.deleteByFollowerId_IdAndFolloweeId_Id(blockerId, targetId);

        // 2. 상대방이 나를 팔로우하고 있다면 언팔로우
        followRepo.deleteByFollowerId_IdAndFolloweeId_Id(targetId, blockerId);

        // 3. 서로의 팔로우 요청 삭제 (양방향)
        reqRepo.deleteByBothUsers(blockerId, targetId);

        // 4. 서로의 리액션 삭제 (양방향)
        recordReactionRepo.deleteByBothUsers(blockerId, targetId);
    }

    // ✅ 차단 해제
    @Transactional
    public void unblockUser(Long blockerId, Long targetId) {
        // 차단 관계가 존재하는지 확인
        if (!userBlockRepo.existsByBlocker_IdAndBlocked_Id(blockerId, targetId)) {
            throw new IllegalArgumentException("차단되지 않은 사용자입니다");
        }

        // 차단 해제
        userBlockRepo.deleteByBlocker_IdAndBlocked_Id(blockerId, targetId);
    }

    // ✅ 차단된 사용자 목록 조회
    @Transactional(readOnly = true)
    public List<BlockedUserDto> getBlockedUsers(Long blockerId) {
        return userBlockRepo.findByBlocker_IdOrderByCreatedAtDesc(blockerId)
                .stream()
                .map(userBlock -> new BlockedUserDto(
                        userBlock.getBlocked().getId(),
                        userBlock.getBlocked().getNickname(),
                        userBlock.getBlocked().getProfileImageUrl(),
                        userBlock.getBlocked().getFavTeam(),
                        userBlock.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    // ✅ 차단 여부 확인 (다른 메서드에서 사용할 유틸리티 메서드)
    public boolean isBlocked(Long blockerId, Long targetId) {
        return userBlockRepo.existsByBlocker_IdAndBlocked_Id(blockerId, targetId);
    }

    // ✅ 상호 차단 여부 확인
    public boolean isBlockedEachOther(Long userId1, Long userId2) {
        return userBlockRepo.existsByBlocker_IdAndBlocked_Id(userId1, userId2) ||
                userBlockRepo.existsByBlocker_IdAndBlocked_Id(userId2, userId1);
    }
}
