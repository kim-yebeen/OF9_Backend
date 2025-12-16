package com.nine.baseballdiary.backend.user.service;

import com.nine.baseballdiary.backend.Notifiation.NotificationService;
import com.nine.baseballdiary.backend.S3.S3Service;
import com.nine.baseballdiary.backend.like.RecordLikeRepository;
import com.nine.baseballdiary.backend.record.GameRecord;
import com.nine.baseballdiary.backend.record.GameRecordRepository;
import com.nine.baseballdiary.backend.search.dto.FollowStatus;
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
import com.nine.baseballdiary.backend.auth.service.RefreshTokenService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.HashSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepo;
    private final UserFollowRepository followRepo;
    private final GameRecordRepository recordRepo;
    private final FollowRequestRepository reqRepo;
    private final NotificationService notificationService;
    private final UserBlockRepository userBlockRepo;
    private final RecordLikeRepository likeRepo;  // ✅ RecordReactionRepository → RecordLikeRepository
    private final S3Service s3Service;
    private final RefreshTokenService refreshTokenService;

    // ✅ 1. searchUsers 메서드 수정
    @Transactional(readOnly = true)
    public List<UserDto> searchUsers(Long currentUserId, String q) {
        Pageable pageable = PageRequest.of(0, 30);
        Page<User> userPage = userRepo.findByNicknameContainingIgnoreCaseAndIdNotExcludingBlocked(
                q, currentUserId, pageable
        );

        // ✅ 나를 팔로우하는 사람들의 ID 목록 조회
        List<Long> myFollowerIds = followRepo.findFollowerIds(currentUserId);
        Set<Long> myFollowerIdSet = new HashSet<>(myFollowerIds);

        // ✅ 내가 팔로우하는 사람들의 ID 목록 조회
        List<Long> myFollowingIds = followRepo.findFollowingIds(currentUserId);
        Set<Long> myFollowingIdSet = new HashSet<>(myFollowingIds);

        List<Long> targetUserIds = userPage.getContent().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        Set<Long> followingIds = followRepo.findFolloweeIdsByFollowerIdAndInTargetUserIds(currentUserId, targetUserIds);
        Set<Long> pendingIds = reqRepo.findPendingRequestTargetIdsByRequesterIdAndInTargetUserIds(currentUserId, targetUserIds);

        return userPage.getContent().stream()
                .map(user -> {
                    FollowStatus status;
                    if (user.getId().equals(currentUserId)) {
                        status = FollowStatus.ME;
                    } else if (followingIds.contains(user.getId())) {
                        status = FollowStatus.FOLLOWING;
                    } else if (pendingIds.contains(user.getId())) {
                        status = FollowStatus.REQUESTED;
                    } else {
                        status = FollowStatus.NOT_FOLLOWING;
                    }

                    // ✅ isMutualFollow 계산
                    // 내가 팔로우하지 않고 있고 && 상대방이 나를 팔로우하고 있으면 true
                    Boolean isMutualFollow = !myFollowingIdSet.contains(user.getId())
                            && myFollowerIdSet.contains(user.getId());

                    return UserDto.from(user, status, isMutualFollow);
                })
                .collect(Collectors.toList());
    }

    // ✅ 2. getFollowers 메서드 수정
    @Transactional(readOnly = true)
    public List<UserDto> getFollowers(Long profileUserId, Long currentUserId) {
        List<Long> followerIds = followRepo.findByFollowee_Id(profileUserId).stream()
                .map(follow -> follow.getFollower().getId())
                .collect(Collectors.toList());

        if (followerIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<User> filteredUserList = userRepo.findByIdInExcludingBlocked(followerIds, currentUserId);

        // ✅ 나를 팔로우하는 사람들의 ID 목록 조회
        List<Long> myFollowerIds = followRepo.findFollowerIds(currentUserId);
        Set<Long> myFollowerIdSet = new HashSet<>(myFollowerIds);

        // ✅ 내가 팔로우하는 사람들의 ID 목록 조회
        List<Long> myFollowingIds = followRepo.findFollowingIds(currentUserId);
        Set<Long> myFollowingIdSet = new HashSet<>(myFollowingIds);

        List<Long> targetUserIds = filteredUserList.stream().map(User::getId).collect(Collectors.toList());
        Set<Long> followingIdSet = followRepo.findFolloweeIdsByFollowerIdAndInTargetUserIds(currentUserId, targetUserIds);
        Set<Long> requestedIdSet = reqRepo.findPendingRequestTargetIdsByRequesterIdAndInTargetUserIds(currentUserId, targetUserIds);

        return filteredUserList.stream().map(user -> {
            FollowStatus status;
            if (user.getId().equals(currentUserId)) {
                status = FollowStatus.ME;
            } else if (followingIdSet.contains(user.getId())) {
                status = FollowStatus.FOLLOWING;
            } else if (requestedIdSet.contains(user.getId())) {
                status = FollowStatus.REQUESTED;
            } else {
                status = FollowStatus.NOT_FOLLOWING;
            }

            // ✅ isMutualFollow 계산
            Boolean isMutualFollow = !myFollowingIdSet.contains(user.getId())
                    && myFollowerIdSet.contains(user.getId());

            return UserDto.from(user, status, isMutualFollow);
        }).collect(Collectors.toList());
    }

    // ✅ 3. getFollowing 메서드 수정
    @Transactional(readOnly = true)
    public List<UserDto> getFollowing(Long profileUserId, Long currentUserId) {
        List<Long> followingIds = followRepo.findByFollower_Id(profileUserId).stream()
                .map(follow -> follow.getFollowee().getId())
                .collect(Collectors.toList());

        if (followingIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<User> filteredUserList = userRepo.findByIdInExcludingBlocked(followingIds, currentUserId);

        // ✅ 나를 팔로우하는 사람들의 ID 목록 조회
        List<Long> myFollowerIds = followRepo.findFollowerIds(currentUserId);
        Set<Long> myFollowerIdSet = new HashSet<>(myFollowerIds);

        // ✅ 내가 팔로우하는 사람들의 ID 목록 조회
        List<Long> myFollowingIds = followRepo.findFollowingIds(currentUserId);
        Set<Long> myFollowingIdSet = new HashSet<>(myFollowingIds);

        List<Long> targetUserIds = filteredUserList.stream().map(User::getId).collect(Collectors.toList());
        Set<Long> followingIdSet = followRepo.findFolloweeIdsByFollowerIdAndInTargetUserIds(currentUserId, targetUserIds);
        Set<Long> requestedIdSet = reqRepo.findPendingRequestTargetIdsByRequesterIdAndInTargetUserIds(currentUserId, targetUserIds);

        return filteredUserList.stream().map(user -> {
            FollowStatus status;
            if (user.getId().equals(currentUserId)) {
                status = FollowStatus.ME;
            } else if (followingIdSet.contains(user.getId())) {
                status = FollowStatus.FOLLOWING;
            } else if (requestedIdSet.contains(user.getId())) {
                status = FollowStatus.REQUESTED;
            } else {
                status = FollowStatus.NOT_FOLLOWING;
            }

            // ✅ isMutualFollow 계산
            Boolean isMutualFollow = !myFollowingIdSet.contains(user.getId())
                    && myFollowerIdSet.contains(user.getId());

            return UserDto.from(user, status, isMutualFollow);
        }).collect(Collectors.toList());
    }

    private List<UserDto> convertToUserDtoWithFollowStatus(List<User> userList, Long currentUserId) {
        if (userList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> targetUserIds = userList.stream().map(User::getId).collect(Collectors.toList());
        Set<Long> followingIdSet = followRepo.findFolloweeIdsByFollowerIdAndInTargetUserIds(currentUserId, targetUserIds);
        Set<Long> requestedIdSet = reqRepo.findPendingRequestTargetIdsByRequesterIdAndInTargetUserIds(currentUserId, targetUserIds);

        return userList.stream().map(user -> {
            FollowStatus status;
            if (user.getId().equals(currentUserId)) {
                status = FollowStatus.ME;
            } else if (followingIdSet.contains(user.getId())) {
                status = FollowStatus.FOLLOWING;
            } else if (requestedIdSet.contains(user.getId())) {
                status = FollowStatus.REQUESTED;
            } else {
                status = FollowStatus.NOT_FOLLOWING;
            }
            return UserDto.from(user, status);
        }).collect(Collectors.toList());
    }

    @Transactional
    public FollowResponse requestFollow(Long meId, Long targetId) {
        if (isBlockedEachOther(meId, targetId)) {
            throw new IllegalArgumentException("차단된 사용자와는 팔로우할 수 없습니다");
        }

        User me = userRepo.findById(meId).orElseThrow();
        User target = userRepo.findById(targetId).orElseThrow();

        if (followRepo.existsByFollower_IdAndFollowee_Id(meId, targetId)) {
            boolean isFollower = followRepo.existsByFollower_IdAndFollowee_Id(targetId, meId);
            return FollowResponse.builder()
                    .followed(false)
                    .pending(false)
                    .requestId(null)
                    .isFollowing(true)
                    .isFollower(isFollower)
                    .isMutual(isFollower)
                    .build();
        }

        if (Boolean.TRUE.equals(target.getIsPrivate())) {
            FollowRequest req = FollowRequest.builder()
                    .requester(me)
                    .target(target)
                    .status(FollowRequestStatus.PENDING)
                    .build();
            req = reqRepo.save(req);
            notificationService.createFollowRequestNotification(targetId, meId);
            boolean isFollower = followRepo.existsByFollower_IdAndFollowee_Id(targetId, meId);

            return FollowResponse.builder()
                    .followed(true)
                    .pending(true)
                    .requestId(req.getId())
                    .isFollowing(false)
                    .isFollower(isFollower)
                    .isMutual(false)
                    .build();
        } else {
            UserFollow userFollow = new UserFollow();
            userFollow.setFollower(me);
            userFollow.setFollowee(target);
            userFollow.setCreatedAt(LocalDateTime.now());
            followRepo.save(userFollow);

            notificationService.createFollowNotification(targetId, meId);
            boolean isFollower = followRepo.existsByFollower_IdAndFollowee_Id(targetId, meId);

            return FollowResponse.builder()
                    .followed(true)
                    .pending(false)
                    .requestId(null)
                    .isFollowing(true)
                    .isFollower(isFollower)
                    .isMutual(isFollower)
                    .build();
        }
    }

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

    @Transactional
    public void acceptFollowRequest(Long me, Long requestId) {
        FollowRequest req = reqRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 요청입니다."));

        if (!req.getTarget().getId().equals(me)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "내 계정으로 들어온 요청만 승인할 수 있습니다."
            );
        }

        if (req.getStatus() != FollowRequestStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이미 처리된 요청입니다."
            );
        }

        UserFollow userFollow = new UserFollow();
        userFollow.setFollower(req.getRequester());
        userFollow.setFollowee(req.getTarget());
        userFollow.setCreatedAt(LocalDateTime.now());
        followRepo.save(userFollow);

        req.setStatus(FollowRequestStatus.ACCEPTED);
    }

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

    @Transactional
    public void unfollow(Long meId, Long targetId) {
        followRepo.deleteByFollower_IdAndFollowee_Id(meId, targetId);
        reqRepo.deleteByRequester_IdAndTarget_IdAndStatus(meId, targetId, FollowRequestStatus.PENDING);
    }

    public UserProfileDto getMyProfile(Long userId) {
        User u = userRepo.findById(userId).orElseThrow();
        long followerCnt = followRepo.countByFollowee_Id(userId);
        long followingCnt = followRepo.countByFollower_Id(userId);
        long recordCnt = recordRepo.countByUserId(userId);
        return new UserProfileDto(
                u.getId(), u.getNickname(), u.getProfileImageUrl(),
                u.getFavTeam(), u.getIsPrivate(),
                followerCnt, followingCnt, recordCnt, u.getPushEnabled()
        );
    }

    // updatePushEnabled 메서드 추가
    @Transactional
    public void updatePushEnabled(Long userId, Boolean pushEnabled) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));

        user.updatePushEnabled(pushEnabled);
        userRepo.save(user);
    }

    @Transactional
    public void updateUser(Long userId, UpdateUserRequest req) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!u.getNickname().equals(req.getNickname())
                && userRepo.existsByNickname(req.getNickname())) {
            throw new IllegalArgumentException("이미 사용중인 닉네임입니다.");
        }

        u.setNickname(req.getNickname());
        u.setFavTeam(req.getFavTeam());
        u.setProfileImageUrl(req.getProfileImageUrl() != null && !req.getProfileImageUrl().trim().isEmpty()
                ? req.getProfileImageUrl().trim() : null);

        if (req.getIsPrivate() != null && !req.getIsPrivate().equals(u.getIsPrivate())) {
            if (Boolean.FALSE.equals(req.getIsPrivate()) && Boolean.TRUE.equals(u.getIsPrivate())) {
                List<FollowRequest> pendingRequests = reqRepo.findByTarget_IdAndStatus(userId, FollowRequestStatus.PENDING);

                for (FollowRequest request : pendingRequests) {
                    UserFollow userFollow = new UserFollow();
                    userFollow.setFollower(request.getRequester());
                    userFollow.setFollowee(request.getTarget());
                    userFollow.setCreatedAt(LocalDateTime.now());
                    followRepo.save(userFollow);

                    request.setStatus(FollowRequestStatus.ACCEPTED);
                    notificationService.createFollowNotification(userId, request.getRequester().getId());
                }
            }

            u.setIsPrivate(req.getIsPrivate());
        } else if (req.getIsPrivate() != null) {
            u.setIsPrivate(req.getIsPrivate());
        }
    }


    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 1. S3 파일 삭제 및 DB 연관 데이터 삭제 (기존 로직 유지)
        List<GameRecord> userRecords = recordRepo.findByUserId(userId);
        userRecords.forEach(record -> {
            if (record.getMediaUrls() != null) {
                record.getMediaUrls().forEach(s3Service::deleteFile);
            }
        });
        if (user.getProfileImageUrl() != null) {
            s3Service.deleteFile(user.getProfileImageUrl());
        }

        likeRepo.deleteAllByUserId(userId);
        recordRepo.deleteAll(userRecords);
        reqRepo.deleteAllByRequesterIdOrTargetId(userId);
        userBlockRepo.deleteAllByBlockerIdOrBlockedId(userId);
        followRepo.deleteAllByFollowerIdOrFolloweeId(userId);

        // ✅ 2. [추가] Redis에 저장된 Refresh Token 삭제 (보안 강화)
        // 탈퇴한 유저의 토큰이 살아있으면 안 되니까요.
        refreshTokenService.deleteRefreshToken(userId.toString());

        // 3. 사용자 엔티티 삭제
        userRepo.delete(user);
    }

    public boolean isNicknameAvailable(String nickname) {
        if (nickname == null || nickname.trim().isEmpty() || nickname.length() > 15) {
            return false;
        }
        return !userRepo.existsByNickname(nickname);
    }

    @Transactional
    public void blockUser(Long blockerId, Long targetId) {
        if (blockerId.equals(targetId)) throw new IllegalArgumentException("자기 자신을 차단할 수 없습니다");
        User target = userRepo.findById(targetId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));
        User blocker = userRepo.findById(blockerId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));
        if (userBlockRepo.existsByBlocker_IdAndBlocked_Id(blockerId, targetId)) throw new IllegalArgumentException("이미 차단된 사용자입니다");

        userBlockRepo.save(UserBlock.builder().blocker(blocker).blocked(target).build());

        followRepo.deleteByFollower_IdAndFollowee_Id(blockerId, targetId);
        followRepo.deleteByFollower_IdAndFollowee_Id(targetId, blockerId);
        reqRepo.deleteByBothUsers(blockerId, targetId);

        // ✅ RecordReactionRepository → RecordLikeRepository
        likeRepo.deleteByBothUsers(blockerId, targetId);
    }

    @Transactional
    public void unblockUser(Long blockerId, Long targetId) {
        if (!userBlockRepo.existsByBlocker_IdAndBlocked_Id(blockerId, targetId)) {
            throw new IllegalArgumentException("차단되지 않은 사용자입니다");
        }
        userBlockRepo.deleteByBlocker_IdAndBlocked_Id(blockerId, targetId);
    }

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

    public boolean isBlockedEachOther(Long userId1, Long userId2) {
        return userBlockRepo.existsByBlocker_IdAndBlocked_Id(userId1, userId2) ||
                userBlockRepo.existsByBlocker_IdAndBlocked_Id(userId2, userId1);
    }

    @Transactional
    public void updateFcmToken(Long userId, String token) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.updateFcmToken(token);
    }
}
